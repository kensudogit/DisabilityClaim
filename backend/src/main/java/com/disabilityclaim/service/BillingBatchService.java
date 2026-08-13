package com.disabilityclaim.service;

import com.disabilityclaim.calculation.CalculationEngine;
import com.disabilityclaim.domain.entity.*;
import com.disabilityclaim.domain.enums.BillingBatchStatus;
import com.disabilityclaim.domain.enums.BillingCaseStatus;
import com.disabilityclaim.domain.enums.ValidationSeverity;
import com.disabilityclaim.export.KokuhoExportAdapter;
import com.disabilityclaim.repository.*;
import com.disabilityclaim.validation.ValidationEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingBatchService {

    private final BillingBatchRepository batchRepository;
    private final BillingCaseRepository caseRepository;
    private final BillingValidationRepository validationRepository;
    private final BillingExportRepository exportRepository;
    private final OfficeProfileRepository officeProfileRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final RecipientCertificateRepository certificateRepository;
    private final MunicipalityRepository municipalityRepository;
    private final FeeRuleSetRepository feeRuleSetRepository;
    private final BillingCandidateService candidateService;
    private final CalculationEngine calculationEngine;
    private final ValidationEngine validationEngine;
    private final KokuhoExportAdapter kokuhoExportAdapter;
    private final AuditService auditService;
    private final UserAccountRepository userAccountRepository;

    @Transactional
    public BillingBatch createDraft(UUID officeId, String billingMonth, UUID actorId) {
        YearMonth.parse(billingMonth);
        OfficeProfile office = officeProfileRepository.findById(officeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "office not found"));
        batchRepository.findByOfficeIdAndBillingMonth(officeId, billingMonth).ifPresent(b -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "batch already exists for month");
        });

        UserAccount actor = actorId == null ? null : userAccountRepository.findById(actorId).orElse(null);
        LocalDate onDate = YearMonth.parse(billingMonth).atEndOfMonth();
        FeeRuleSet ruleSet = feeRuleSetRepository.findEffectiveOn(onDate).stream().findFirst().orElse(null);

        BillingBatch batch = BillingBatch.builder()
                .office(office)
                .billingMonth(billingMonth)
                .status(BillingBatchStatus.DRAFT)
                .feeRuleSet(ruleSet)
                .createdBy(actor)
                .build();
        batch = batchRepository.save(batch);

        List<BillingCandidateService.BillingCandidate> candidates =
                candidateService.findCandidates(billingMonth, officeId);
        for (BillingCandidateService.BillingCandidate candidate : candidates) {
            Beneficiary beneficiary = beneficiaryRepository.findById(candidate.getBeneficiaryId()).orElseThrow();
            RecipientCertificate certificate = certificateRepository.findById(candidate.getCertificateId()).orElseThrow();
            Municipality municipality = municipalityRepository.findById(candidate.getMunicipalityId()).orElse(null);
            caseRepository.save(BillingCase.builder()
                    .batch(batch)
                    .beneficiary(beneficiary)
                    .certificate(certificate)
                    .municipality(municipality)
                    .serviceCategory(candidate.getServiceCategory())
                    .category(candidate.getCategory())
                    .status(BillingCaseStatus.CANDIDATE)
                    .confirmed(false)
                    .build());
        }
        auditService.record(actorId, actor != null ? actor.getUsername() : null,
                "CREATE_BATCH", "BillingBatch", batch.getId().toString(), null, billingMonth, null);
        return batch;
    }

    @Transactional
    public BillingBatch calculate(UUID batchId) {
        BillingBatch batch = getBatch(batchId);
        assertStatus(batch, BillingBatchStatus.DRAFT, BillingBatchStatus.CALCULATED, BillingBatchStatus.VALIDATED);
        List<BillingCase> cases = caseRepository.findByBatchId(batchId);
        String region = batch.getOffice().getRegionCategoryCode();
        for (BillingCase billingCase : cases) {
            calculationEngine.calculate(billingCase, batch.getBillingMonth(), null, region);
            caseRepository.save(billingCase);
        }
        batch.setStatus(BillingBatchStatus.CALCULATED);
        return batchRepository.save(batch);
    }

    @Transactional
    public BillingBatch validate(UUID batchId) {
        BillingBatch batch = getBatch(batchId);
        assertStatus(batch, BillingBatchStatus.CALCULATED, BillingBatchStatus.VALIDATED);
        List<BillingCase> cases = caseRepository.findByBatchId(batchId);
        validationEngine.validate(batch, cases);
        batch.setStatus(BillingBatchStatus.VALIDATED);
        return batchRepository.save(batch);
    }

    @Transactional
    public BillingBatch confirm(UUID batchId, UUID actorId) {
        BillingBatch batch = getBatch(batchId);
        assertStatus(batch, BillingBatchStatus.VALIDATED);
        if (validationRepository.existsByBatchIdAndSeverity(batchId, ValidationSeverity.ERROR)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot confirm batch while ERROR validations exist");
        }
        UserAccount actor = actorId == null ? null : userAccountRepository.findById(actorId).orElse(null);
        batch.setStatus(BillingBatchStatus.CONFIRMED);
        batch.setConfirmedBy(actor);
        batch.setConfirmedAt(Instant.now());
        List<BillingCase> cases = caseRepository.findByBatchId(batchId);
        for (BillingCase c : cases) {
            c.setConfirmed(true);
            c.setStatus(BillingCaseStatus.CONFIRMED);
            caseRepository.save(c);
        }
        auditService.record(actorId, actor != null ? actor.getUsername() : null,
                "CONFIRM_BATCH", "BillingBatch", batchId.toString(), null, "CONFIRMED", null);
        return batchRepository.save(batch);
    }

    @Transactional
    public BillingBatch reopen(UUID batchId, UUID actorId) {
        BillingBatch batch = getBatch(batchId);
        if (batch.getStatus() != BillingBatchStatus.CONFIRMED && batch.getStatus() != BillingBatchStatus.VALIDATED
                && batch.getStatus() != BillingBatchStatus.CALCULATED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot reopen from status " + batch.getStatus());
        }
        if (batch.getStatus() == BillingBatchStatus.EXPORTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot reopen exported batch");
        }
        batch.setStatus(BillingBatchStatus.DRAFT);
        batch.setConfirmedAt(null);
        batch.setConfirmedBy(null);
        List<BillingCase> cases = caseRepository.findByBatchId(batchId);
        for (BillingCase c : cases) {
            c.setConfirmed(false);
            c.setStatus(BillingCaseStatus.CANDIDATE);
            caseRepository.save(c);
        }
        auditService.record(actorId, null, "REOPEN_BATCH", "BillingBatch", batchId.toString(),
                "CONFIRMED/VALIDATED", "DRAFT", null);
        return batchRepository.save(batch);
    }

    @Transactional
    public BillingExport exportKokuho(UUID batchId, UUID actorId) {
        BillingBatch batch = getBatch(batchId);
        if (batch.getStatus() != BillingBatchStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Batch must be CONFIRMED before export");
        }
        if (validationRepository.existsByBatchIdAndSeverity(batchId, ValidationSeverity.ERROR)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ERROR validations block export");
        }
        UserAccount actor = actorId == null ? null : userAccountRepository.findById(actorId).orElse(null);
        BillingExport export = BillingExport.builder()
                .batch(batch)
                .exportType("KOKUHO")
                .status("PENDING")
                .adapterName(kokuhoExportAdapter.name())
                .createdBy(actor)
                .build();
        export = exportRepository.save(export);
        try {
            kokuhoExportAdapter.export(batch);
            export.setStatus("SUCCESS");
            batch.setStatus(BillingBatchStatus.EXPORTED);
            batchRepository.save(batch);
        } catch (IllegalStateException ex) {
            export.setStatus("UNSUPPORTED");
            export.setErrorMessage(ex.getMessage());
        }
        export.setCompletedAt(Instant.now());
        return exportRepository.save(export);
    }

    @Transactional(readOnly = true)
    public BillingBatch getBatch(UUID batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "batch not found"));
    }

    @Transactional(readOnly = true)
    public List<BillingCase> getCases(UUID batchId) {
        getBatch(batchId);
        return caseRepository.findByBatchId(batchId);
    }

    @Transactional(readOnly = true)
    public List<BillingValidation> getValidations(UUID batchId) {
        getBatch(batchId);
        return validationRepository.findByBatchId(batchId);
    }

    private void assertStatus(BillingBatch batch, BillingBatchStatus... allowed) {
        for (BillingBatchStatus s : allowed) {
            if (batch.getStatus() == s) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Invalid status transition from " + batch.getStatus());
    }
}
