package com.disabilityclaim.service;

import com.disabilityclaim.calculation.CalculationEngine;
import com.disabilityclaim.domain.entity.BillingBatch;
import com.disabilityclaim.domain.entity.BillingCase;
import com.disabilityclaim.domain.entity.BillingExport;
import com.disabilityclaim.domain.entity.OfficeProfile;
import com.disabilityclaim.domain.enums.BillingBatchStatus;
import com.disabilityclaim.domain.enums.BillingCaseStatus;
import com.disabilityclaim.domain.enums.ValidationSeverity;
import com.disabilityclaim.export.KokuhoExportAdapter;
import com.disabilityclaim.repository.*;
import com.disabilityclaim.validation.ValidationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingBatchServiceTest {

    @Mock private BillingBatchRepository batchRepository;
    @Mock private BillingCaseRepository caseRepository;
    @Mock private BillingValidationRepository validationRepository;
    @Mock private BillingExportRepository exportRepository;
    @Mock private OfficeProfileRepository officeProfileRepository;
    @Mock private BeneficiaryRepository beneficiaryRepository;
    @Mock private RecipientCertificateRepository certificateRepository;
    @Mock private MunicipalityRepository municipalityRepository;
    @Mock private FeeRuleSetRepository feeRuleSetRepository;
    @Mock private BillingCandidateService candidateService;
    @Mock private CalculationEngine calculationEngine;
    @Mock private ValidationEngine validationEngine;
    @Mock private KokuhoExportAdapter kokuhoExportAdapter;
    @Mock private AuditService auditService;
    @Mock private UserAccountRepository userAccountRepository;

    private BillingBatchService service;

    @BeforeEach
    void setUp() {
        service = new BillingBatchService(
                batchRepository, caseRepository, validationRepository, exportRepository,
                officeProfileRepository, beneficiaryRepository, certificateRepository,
                municipalityRepository, feeRuleSetRepository, candidateService,
                calculationEngine, validationEngine, kokuhoExportAdapter,
                auditService, userAccountRepository);
    }

    @Test
    void confirmBlockedWhenErrorValidationsExist() {
        UUID batchId = UUID.randomUUID();
        BillingBatch batch = BillingBatch.builder()
                .id(batchId)
                .status(BillingBatchStatus.VALIDATED)
                .billingMonth("2026-08")
                .build();
        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(validationRepository.existsByBatchIdAndSeverity(batchId, ValidationSeverity.ERROR)).thenReturn(true);

        assertThatThrownBy(() -> service.confirm(batchId, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot confirm");
    }

    @Test
    void confirmSucceedsWhenNoErrors() {
        UUID batchId = UUID.randomUUID();
        BillingBatch batch = BillingBatch.builder()
                .id(batchId)
                .status(BillingBatchStatus.VALIDATED)
                .billingMonth("2026-08")
                .build();
        BillingCase billingCase = BillingCase.builder()
                .id(UUID.randomUUID())
                .status(BillingCaseStatus.CALCULATED)
                .build();
        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(validationRepository.existsByBatchIdAndSeverity(batchId, ValidationSeverity.ERROR)).thenReturn(false);
        when(caseRepository.findByBatchId(batchId)).thenReturn(List.of(billingCase));
        when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BillingBatch result = service.confirm(batchId, null);
        assertThat(result.getStatus()).isEqualTo(BillingBatchStatus.CONFIRMED);
        assertThat(billingCase.isConfirmed()).isTrue();
        verify(auditService).record(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void exportKokuhoRecordsUnsupportedWhenAdapterThrows() {
        UUID batchId = UUID.randomUUID();
        BillingBatch batch = BillingBatch.builder()
                .id(batchId)
                .status(BillingBatchStatus.CONFIRMED)
                .billingMonth("2026-08")
                .build();
        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(validationRepository.existsByBatchIdAndSeverity(batchId, ValidationSeverity.ERROR)).thenReturn(false);
        when(kokuhoExportAdapter.name()).thenReturn("UnsupportedKokuhoExportAdapter");
        when(kokuhoExportAdapter.export(batch)).thenThrow(new IllegalStateException("公式I/F仕様未提供"));
        when(exportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BillingExport export = service.exportKokuho(batchId, null);
        assertThat(export.getStatus()).isEqualTo("UNSUPPORTED");
        assertThat(export.getErrorMessage()).contains("公式I/F仕様未提供");
        assertThat(batch.getStatus()).isEqualTo(BillingBatchStatus.CONFIRMED);
    }

    @Test
    void reopenFromConfirmedReturnsDraft() {
        UUID batchId = UUID.randomUUID();
        BillingBatch batch = BillingBatch.builder()
                .id(batchId)
                .status(BillingBatchStatus.CONFIRMED)
                .billingMonth("2026-08")
                .build();
        BillingCase billingCase = BillingCase.builder()
                .id(UUID.randomUUID())
                .confirmed(true)
                .status(BillingCaseStatus.CONFIRMED)
                .build();
        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(caseRepository.findByBatchId(batchId)).thenReturn(List.of(billingCase));
        when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BillingBatch result = service.reopen(batchId, null);
        assertThat(result.getStatus()).isEqualTo(BillingBatchStatus.DRAFT);
        assertThat(billingCase.isConfirmed()).isFalse();
        assertThat(billingCase.getStatus()).isEqualTo(BillingCaseStatus.CANDIDATE);
    }

    @Test
    void getBatchNotFound() {
        UUID batchId = UUID.randomUUID();
        when(batchRepository.findById(batchId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getBatch(batchId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("batch not found");
    }

    @Test
    void createDraftRejectsUnknownOffice() {
        UUID officeId = UUID.randomUUID();
        when(officeProfileRepository.findById(officeId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createDraft(officeId, "2026-08", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("office not found");
    }
}
