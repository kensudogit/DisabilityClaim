package com.disabilityclaim.validation.rules;

import com.disabilityclaim.domain.entity.Beneficiary;
import com.disabilityclaim.domain.entity.BillingCase;
import com.disabilityclaim.domain.entity.Municipality;
import com.disabilityclaim.domain.entity.RecipientCertificate;
import com.disabilityclaim.domain.enums.BeneficiaryCategory;
import com.disabilityclaim.domain.enums.BillingCaseStatus;
import com.disabilityclaim.domain.enums.ServiceCategory;
import com.disabilityclaim.domain.enums.ValidationSeverity;
import com.disabilityclaim.repository.SupportActivityRepository;
import com.disabilityclaim.validation.ValidationContext;
import com.disabilityclaim.validation.ValidationFinding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationRulesTest {

    @Mock
    private SupportActivityRepository supportActivityRepository;

    private ValidationContext ctx(String month, BillingCase... cases) {
        return ValidationContext.builder()
                .billingMonth(month)
                .cases(List.of(cases))
                .build();
    }

    @Test
    void missingRequiredFieldDetectsNulls() {
        BillingCase c = BillingCase.builder().id(UUID.randomUUID()).build();
        List<ValidationFinding> findings = new MissingRequiredFieldRule().validate(ctx("2026-08", c));
        assertThat(findings).hasSize(3);
        assertThat(findings).allMatch(f -> f.severity() == ValidationSeverity.ERROR);
        assertThat(findings).extracting(ValidationFinding::ruleCode).containsOnly("MISSING_REQUIRED_FIELD");
    }

    @Test
    void certificateExpiredWhenOutsideMonth() {
        RecipientCertificate cert = RecipientCertificate.builder()
                .id(UUID.randomUUID())
                .validFrom(LocalDate.of(2025, 1, 1))
                .validTo(LocalDate.of(2025, 12, 31))
                .build();
        BillingCase c = BillingCase.builder().id(UUID.randomUUID()).certificate(cert).build();
        List<ValidationFinding> findings = new CertificateExpiredRule().validate(ctx("2026-08", c));
        assertThat(findings).hasSize(1);
        assertThat(findings.getFirst().ruleCode()).isEqualTo("CERTIFICATE_EXPIRED");
    }

    @Test
    void certificateExpiredPassesWhenValidInMonth() {
        RecipientCertificate cert = RecipientCertificate.builder()
                .id(UUID.randomUUID())
                .validFrom(LocalDate.of(2026, 1, 1))
                .validTo(LocalDate.of(2026, 12, 31))
                .build();
        BillingCase c = BillingCase.builder().id(UUID.randomUUID()).certificate(cert).build();
        assertThat(new CertificateExpiredRule().validate(ctx("2026-08", c))).isEmpty();
    }

    @Test
    void periodMismatchWhenNoOverlap() {
        RecipientCertificate cert = RecipientCertificate.builder()
                .id(UUID.randomUUID())
                .validFrom(LocalDate.of(2024, 1, 1))
                .validTo(LocalDate.of(2024, 6, 30))
                .build();
        BillingCase c = BillingCase.builder().id(UUID.randomUUID()).certificate(cert).build();
        assertThat(new PeriodMismatchRule().validate(ctx("2026-08", c)))
                .extracting(ValidationFinding::ruleCode)
                .containsExactly("PERIOD_MISMATCH");
    }

    @Test
    void missingActivityWhenNoSupportRecord() {
        UUID beneficiaryId = UUID.randomUUID();
        Beneficiary beneficiary = Beneficiary.builder().id(beneficiaryId).build();
        BillingCase c = BillingCase.builder().id(UUID.randomUUID()).beneficiary(beneficiary).build();
        when(supportActivityRepository.existsByBeneficiaryIdAndBillingMonth(eq(beneficiaryId), eq("2026-08")))
                .thenReturn(false);

        List<ValidationFinding> findings = new MissingActivityRule(supportActivityRepository)
                .validate(ctx("2026-08", c));
        assertThat(findings).hasSize(1);
        assertThat(findings.getFirst().ruleCode()).isEqualTo("MISSING_ACTIVITY");
    }

    @Test
    void missingActivityPassesWhenRecordExists() {
        UUID beneficiaryId = UUID.randomUUID();
        BillingCase c = BillingCase.builder()
                .id(UUID.randomUUID())
                .beneficiary(Beneficiary.builder().id(beneficiaryId).build())
                .build();
        when(supportActivityRepository.existsByBeneficiaryIdAndBillingMonth(any(), any())).thenReturn(true);
        assertThat(new MissingActivityRule(supportActivityRepository).validate(ctx("2026-08", c))).isEmpty();
    }

    @Test
    void duplicateBillingSuspectWarnsOnSameBeneficiaryTwice() {
        UUID bid = UUID.randomUUID();
        Beneficiary b = Beneficiary.builder().id(bid).build();
        BillingCase c1 = BillingCase.builder().id(UUID.randomUUID()).beneficiary(b).build();
        BillingCase c2 = BillingCase.builder().id(UUID.randomUUID()).beneficiary(b).build();
        List<ValidationFinding> findings = new DuplicateBillingSuspectRule().validate(ctx("2026-08", c1, c2));
        assertThat(findings).hasSize(1);
        assertThat(findings.getFirst().severity()).isEqualTo(ValidationSeverity.WARNING);
        assertThat(findings.getFirst().ruleCode()).isEqualTo("DUPLICATE_BILLING_SUSPECT");
    }

    @Test
    void serviceCategoryMismatchChildWithAdultService() {
        BillingCase c = BillingCase.builder()
                .id(UUID.randomUUID())
                .category(BeneficiaryCategory.CHILD)
                .serviceCategory(ServiceCategory.PLAN_CONSULTATION)
                .build();
        assertThat(new ServiceCategoryMismatchRule().validate(ctx("2026-08", c)))
                .extracting(ValidationFinding::ruleCode)
                .containsExactly("SERVICE_CATEGORY_MISMATCH");
    }

    @Test
    void serviceCategoryMismatchAdultWithChildService() {
        BillingCase c = BillingCase.builder()
                .id(UUID.randomUUID())
                .category(BeneficiaryCategory.ADULT)
                .serviceCategory(ServiceCategory.CHILD_CONSULTATION)
                .build();
        assertThat(new ServiceCategoryMismatchRule().validate(ctx("2026-08", c))).isNotEmpty();
    }

    @Test
    void calculationInconsistencyOnNegativeUnitsAndAmount() {
        BillingCase c = BillingCase.builder()
                .id(UUID.randomUUID())
                .status(BillingCaseStatus.CALCULATED)
                .totalUnits(-1)
                .billedAmount(new BigDecimal("-10"))
                .build();
        List<ValidationFinding> findings = new CalculationInconsistencyRule().validate(ctx("2026-08", c));
        assertThat(findings).hasSize(2);
        assertThat(findings).extracting(ValidationFinding::ruleCode).containsOnly("CALCULATION_INCONSISTENCY");
    }

    @Test
    void needsRuleDataWhenCaseStatusIsNeedsRuleData() {
        BillingCase ok = BillingCase.builder().id(UUID.randomUUID()).status(BillingCaseStatus.CALCULATED).build();
        BillingCase bad = BillingCase.builder().id(UUID.randomUUID()).status(BillingCaseStatus.NEEDS_RULE_DATA).build();
        List<ValidationFinding> findings = new NeedsRuleDataRule().validate(ctx("2026-08", ok, bad));
        assertThat(findings).hasSize(1);
        assertThat(findings.getFirst().ruleCode()).isEqualTo("NEEDS_RULE_DATA");
    }

    @Test
    void missingRequiredFieldPassesWhenAllPresent() {
        BillingCase c = BillingCase.builder()
                .id(UUID.randomUUID())
                .beneficiary(Beneficiary.builder().id(UUID.randomUUID()).build())
                .certificate(RecipientCertificate.builder().id(UUID.randomUUID())
                        .validFrom(LocalDate.of(2026, 1, 1))
                        .validTo(LocalDate.of(2026, 12, 31))
                        .build())
                .municipality(Municipality.builder().id(UUID.randomUUID()).code("999001").name("Demo").build())
                .build();
        assertThat(new MissingRequiredFieldRule().validate(ctx("2026-08", c))).isEmpty();
    }
}
