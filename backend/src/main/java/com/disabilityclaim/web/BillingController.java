package com.disabilityclaim.web;

import com.disabilityclaim.domain.entity.BillingBatch;
import com.disabilityclaim.domain.entity.BillingCase;
import com.disabilityclaim.domain.entity.BillingExport;
import com.disabilityclaim.domain.entity.BillingValidation;
import com.disabilityclaim.domain.entity.UserAccount;
import com.disabilityclaim.repository.UserAccountRepository;
import com.disabilityclaim.service.BillingBatchService;
import com.disabilityclaim.service.BillingCandidateService;
import com.disabilityclaim.web.dto.BillingDtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingCandidateService candidateService;
    private final BillingBatchService batchService;
    private final UserAccountRepository userAccountRepository;

    public record CreateBatchRequest(@NotNull UUID officeId, @NotBlank String month) {
    }

    @GetMapping("/candidates")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_MANAGER','BILLING_OPERATOR','VIEWER')")
    public List<BillingCandidateService.BillingCandidate> candidates(
            @RequestParam String month,
            @RequestParam(required = false) UUID officeId) {
        return candidateService.findCandidates(month, officeId);
    }

    @PostMapping("/batches")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_MANAGER','BILLING_OPERATOR')")
    public BillingDtos.BatchSummary createBatch(@RequestBody CreateBatchRequest request, Authentication authentication) {
        UserAccount actor = currentUser(authentication);
        return toBatchSummary(batchService.createDraft(request.officeId(), request.month(), actor.getId()));
    }

    @GetMapping("/batches/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_MANAGER','BILLING_OPERATOR','VIEWER')")
    public Map<String, Object> getBatch(@PathVariable UUID id) {
        BillingBatch batch = batchService.getBatch(id);
        List<BillingDtos.CaseSummary> cases = batchService.getCases(id).stream().map(this::toCaseSummary).toList();
        return Map.of(
                "batch", toBatchSummary(batch),
                "cases", cases
        );
    }

    @PostMapping("/batches/{id}/calculate")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_MANAGER','BILLING_OPERATOR')")
    public BillingDtos.BatchSummary calculate(@PathVariable UUID id) {
        return toBatchSummary(batchService.calculate(id));
    }

    @PostMapping("/batches/{id}/validate")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_MANAGER','BILLING_OPERATOR')")
    public BillingDtos.BatchSummary validate(@PathVariable UUID id) {
        return toBatchSummary(batchService.validate(id));
    }

    @PostMapping("/batches/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_MANAGER')")
    public BillingDtos.BatchSummary confirm(@PathVariable UUID id, Authentication authentication) {
        return toBatchSummary(batchService.confirm(id, currentUser(authentication).getId()));
    }

    @PostMapping("/batches/{id}/reopen")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_MANAGER')")
    public BillingDtos.BatchSummary reopen(@PathVariable UUID id, Authentication authentication) {
        return toBatchSummary(batchService.reopen(id, currentUser(authentication).getId()));
    }

    @PostMapping("/batches/{id}/exports/kokuho")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_MANAGER')")
    public Map<String, Object> exportKokuho(@PathVariable UUID id, Authentication authentication) {
        BillingExport export = batchService.exportKokuho(id, currentUser(authentication).getId());
        return Map.of(
                "id", export.getId(),
                "status", export.getStatus(),
                "adapterName", export.getAdapterName() == null ? "" : export.getAdapterName(),
                "errorMessage", export.getErrorMessage() == null ? "" : export.getErrorMessage()
        );
    }

    @GetMapping("/batches/{id}/validations")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_MANAGER','BILLING_OPERATOR','VIEWER')")
    public List<BillingDtos.ValidationSummary> validations(@PathVariable UUID id) {
        return batchService.getValidations(id).stream().map(this::toValidationSummary).toList();
    }

    private BillingDtos.BatchSummary toBatchSummary(BillingBatch batch) {
        return new BillingDtos.BatchSummary(
                batch.getId(),
                batch.getOffice().getId(),
                batch.getBillingMonth(),
                batch.getStatus());
    }

    private BillingDtos.CaseSummary toCaseSummary(BillingCase c) {
        return new BillingDtos.CaseSummary(
                c.getId(),
                c.getBeneficiary() != null ? c.getBeneficiary().getId() : null,
                c.getCertificate() != null ? c.getCertificate().getId() : null,
                c.getMunicipality() != null ? c.getMunicipality().getId() : null,
                c.getServiceCategory(),
                c.getCategory(),
                c.getStatus(),
                c.getTotalUnits(),
                c.getBilledAmount(),
                c.isConfirmed());
    }

    private BillingDtos.ValidationSummary toValidationSummary(BillingValidation v) {
        return new BillingDtos.ValidationSummary(
                v.getId(),
                v.getBillingCase() != null ? v.getBillingCase().getId() : null,
                v.getRuleCode(),
                v.getSeverity(),
                v.getMessage(),
                v.getFieldName());
    }

    private UserAccount currentUser(Authentication authentication) {
        return userAccountRepository.findByUsername(authentication.getName()).orElseThrow();
    }
}
