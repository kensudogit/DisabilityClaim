package com.disabilityclaim.validation.rules;

import com.disabilityclaim.domain.entity.BillingCase;
import com.disabilityclaim.domain.enums.BillingCaseStatus;
import com.disabilityclaim.domain.enums.ValidationSeverity;
import com.disabilityclaim.validation.ValidationContext;
import com.disabilityclaim.validation.ValidationFinding;
import com.disabilityclaim.validation.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CalculationInconsistencyRule implements ValidationRule {
    @Override
    public String code() {
        return "CALCULATION_INCONSISTENCY";
    }

    @Override
    public List<ValidationFinding> validate(ValidationContext context) {
        List<ValidationFinding> findings = new ArrayList<>();
        for (BillingCase c : context.getCases()) {
            if (c.getStatus() == BillingCaseStatus.CALCULATED
                    && c.getTotalUnits() != null
                    && c.getTotalUnits() < 0) {
                findings.add(new ValidationFinding(code(), ValidationSeverity.ERROR,
                        "Total units is negative", "totalUnits", c));
            }
            if (c.getBilledAmount() != null && c.getBilledAmount().signum() < 0) {
                findings.add(new ValidationFinding(code(), ValidationSeverity.ERROR,
                        "Billed amount is negative", "billedAmount", c));
            }
        }
        return findings;
    }
}
