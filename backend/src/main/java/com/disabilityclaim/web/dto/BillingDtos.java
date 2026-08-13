package com.disabilityclaim.web.dto;

import com.disabilityclaim.domain.enums.BeneficiaryCategory;
import com.disabilityclaim.domain.enums.BillingBatchStatus;
import com.disabilityclaim.domain.enums.BillingCaseStatus;
import com.disabilityclaim.domain.enums.ServiceCategory;
import com.disabilityclaim.domain.enums.ValidationSeverity;

import java.math.BigDecimal;
import java.util.UUID;

public final class BillingDtos {

    private BillingDtos() {
    }

    public record BatchSummary(
            UUID id,
            UUID officeId,
            String billingMonth,
            BillingBatchStatus status
    ) {
    }

    public record CaseSummary(
            UUID id,
            UUID beneficiaryId,
            UUID certificateId,
            UUID municipalityId,
            ServiceCategory serviceCategory,
            BeneficiaryCategory category,
            BillingCaseStatus status,
            Integer totalUnits,
            BigDecimal billedAmount,
            boolean confirmed
    ) {
    }

    public record ValidationSummary(
            UUID id,
            UUID billingCaseId,
            String ruleCode,
            ValidationSeverity severity,
            String message,
            String fieldName
    ) {
    }
}
