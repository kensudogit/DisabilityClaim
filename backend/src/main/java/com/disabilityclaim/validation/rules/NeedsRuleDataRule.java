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
public class NeedsRuleDataRule implements ValidationRule {
    @Override
    public String code() {
        return "NEEDS_RULE_DATA";
    }

    @Override
    public List<ValidationFinding> validate(ValidationContext context) {
        List<ValidationFinding> findings = new ArrayList<>();
        for (BillingCase c : context.getCases()) {
            if (c.getStatus() == BillingCaseStatus.NEEDS_RULE_DATA) {
                findings.add(new ValidationFinding(code(), ValidationSeverity.ERROR,
                        "Fee master data missing or PENDING_OFFICIAL_SPEC; cannot finalize amounts",
                        "feeMasters", c));
            }
        }
        return findings;
    }
}
