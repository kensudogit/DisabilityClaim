package com.disabilityclaim.validation;

import com.disabilityclaim.domain.entity.BillingCase;
import com.disabilityclaim.domain.enums.ValidationSeverity;

public record ValidationFinding(
        String ruleCode,
        ValidationSeverity severity,
        String message,
        String fieldName,
        BillingCase billingCase
) {
}
