package com.disabilityclaim.validation;

import com.disabilityclaim.domain.entity.BillingBatch;
import com.disabilityclaim.domain.entity.BillingCase;
import com.disabilityclaim.domain.enums.ValidationSeverity;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ValidationContext {
    private final BillingBatch batch;
    private final List<BillingCase> cases;
    private final String billingMonth;
}
