package com.disabilityclaim.validation;

import com.disabilityclaim.domain.entity.BillingBatch;
import com.disabilityclaim.domain.entity.BillingCase;
import com.disabilityclaim.domain.entity.Beneficiary;
import com.disabilityclaim.domain.entity.Municipality;
import com.disabilityclaim.domain.entity.RecipientCertificate;
import com.disabilityclaim.domain.enums.BeneficiaryCategory;
import com.disabilityclaim.domain.enums.BillingCaseStatus;
import com.disabilityclaim.domain.enums.ServiceCategory;
import com.disabilityclaim.domain.enums.ValidationSeverity;
import com.disabilityclaim.repository.SupportActivityRepository;
import com.disabilityclaim.validation.rules.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationEngineTest {

    @Mock
    private SupportActivityRepository supportActivityRepository;

    private ValidationEngine engine;

    @BeforeEach
    void setUp() {
        List<ValidationRule> rules = List.of(
                new MissingRequiredFieldRule(),
                new CertificateExpiredRule(),
                new PeriodMismatchRule(),
                new MissingActivityRule(supportActivityRepository),
                new DuplicateBillingSuspectRule(),
                new ServiceCategoryMismatchRule(),
                new CalculationInconsistencyRule(),
                new NeedsRuleDataRule()
        );
        engine = new ValidationEngine(rules, null);
    }

    @Test
    void needsRuleDataProducesError() {
        BillingBatch batch = BillingBatch.builder()
                .id(UUID.randomUUID())
                .billingMonth("2026-08")
                .build();
        BillingCase billingCase = BillingCase.builder()
                .id(UUID.randomUUID())
                .status(BillingCaseStatus.NEEDS_RULE_DATA)
                .municipality(Municipality.builder().id(UUID.randomUUID()).code("999001").name("デモ市A").build())
                .certificate(RecipientCertificate.builder()
                        .id(UUID.randomUUID())
                        .validFrom(LocalDate.of(2026, 1, 1))
                        .validTo(LocalDate.of(2026, 12, 31))
                        .serviceCategory(ServiceCategory.PLAN_CONSULTATION)
                        .build())
                .beneficiary(Beneficiary.builder().id(UUID.randomUUID()).category(BeneficiaryCategory.ADULT).build())
                .category(BeneficiaryCategory.ADULT)
                .serviceCategory(ServiceCategory.PLAN_CONSULTATION)
                .build();

        when(supportActivityRepository.existsByBeneficiaryIdAndBillingMonth(any(), eq("2026-08")))
                .thenReturn(true);

        ValidationContext context = ValidationContext.builder()
                .batch(batch)
                .cases(List.of(billingCase))
                .billingMonth("2026-08")
                .build();

        List<ValidationFinding> findings = engine.validateInMemory(context);
        assertThat(findings).anyMatch(f ->
                "NEEDS_RULE_DATA".equals(f.ruleCode()) && f.severity() == ValidationSeverity.ERROR);
    }

    @Test
    void missingMunicipalityIsError() {
        BillingBatch batch = BillingBatch.builder().id(UUID.randomUUID()).billingMonth("2026-08").build();
        BillingCase billingCase = BillingCase.builder()
                .id(UUID.randomUUID())
                .status(BillingCaseStatus.CANDIDATE)
                .municipality(null)
                .certificate(null)
                .beneficiary(Beneficiary.builder().id(UUID.randomUUID()).build())
                .build();
        when(supportActivityRepository.existsByBeneficiaryIdAndBillingMonth(any(), eq("2026-08")))
                .thenReturn(false);

        List<ValidationFinding> findings = engine.validateInMemory(ValidationContext.builder()
                .batch(batch)
                .cases(List.of(billingCase))
                .billingMonth("2026-08")
                .build());

        assertThat(findings).anyMatch(f -> "MISSING_REQUIRED_FIELD".equals(f.ruleCode()));
        assertThat(findings).anyMatch(f -> "MISSING_ACTIVITY".equals(f.ruleCode()));
    }
}
