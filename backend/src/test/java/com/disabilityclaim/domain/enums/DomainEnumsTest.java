package com.disabilityclaim.domain.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEnumsTest {

    @Test
    void beneficiaryStatuses() {
        assertThat(BeneficiaryStatus.values()).containsExactly(
                BeneficiaryStatus.ACTIVE, BeneficiaryStatus.SUSPENDED, BeneficiaryStatus.CLOSED);
    }

    @Test
    void beneficiaryCategories() {
        assertThat(BeneficiaryCategory.values()).containsExactly(
                BeneficiaryCategory.ADULT, BeneficiaryCategory.CHILD);
    }

    @Test
    void billingBatchStatuses() {
        assertThat(BillingBatchStatus.values()).contains(
                BillingBatchStatus.DRAFT,
                BillingBatchStatus.CALCULATED,
                BillingBatchStatus.VALIDATED,
                BillingBatchStatus.CONFIRMED,
                BillingBatchStatus.EXPORTED);
    }

    @Test
    void validationSeverities() {
        assertThat(ValidationSeverity.values()).contains(
                ValidationSeverity.ERROR, ValidationSeverity.WARNING, ValidationSeverity.INFO);
    }

    @Test
    void serviceCategories() {
        assertThat(ServiceCategory.values()).contains(
                ServiceCategory.PLAN_CONSULTATION,
                ServiceCategory.MONITORING,
                ServiceCategory.CHILD_CONSULTATION,
                ServiceCategory.OTHER);
    }
}
