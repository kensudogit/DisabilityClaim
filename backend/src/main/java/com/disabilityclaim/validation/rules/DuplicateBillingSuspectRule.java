package com.disabilityclaim.validation.rules;

import com.disabilityclaim.domain.entity.BillingCase;
import com.disabilityclaim.domain.enums.ValidationSeverity;
import com.disabilityclaim.validation.ValidationContext;
import com.disabilityclaim.validation.ValidationFinding;
import com.disabilityclaim.validation.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class DuplicateBillingSuspectRule implements ValidationRule {
    @Override
    public String code() {
        return "DUPLICATE_BILLING_SUSPECT";
    }

    @Override
    public List<ValidationFinding> validate(ValidationContext context) {
        List<ValidationFinding> findings = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (BillingCase c : context.getCases()) {
            if (c.getBeneficiary() == null) {
                continue;
            }
            UUID bid = c.getBeneficiary().getId();
            if (!seen.add(bid)) {
                findings.add(new ValidationFinding(code(), ValidationSeverity.WARNING,
                        "Duplicate beneficiary in same billing batch", "beneficiaryId", c));
            }
        }
        return findings;
    }
}
