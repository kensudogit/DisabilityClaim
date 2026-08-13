package com.disabilityclaim.validation.rules;

import com.disabilityclaim.domain.entity.BillingCase;
import com.disabilityclaim.domain.enums.ValidationSeverity;
import com.disabilityclaim.repository.SupportActivityRepository;
import com.disabilityclaim.validation.ValidationContext;
import com.disabilityclaim.validation.ValidationFinding;
import com.disabilityclaim.validation.ValidationRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MissingActivityRule implements ValidationRule {

    private final SupportActivityRepository supportActivityRepository;

    @Override
    public String code() {
        return "MISSING_ACTIVITY";
    }

    @Override
    public List<ValidationFinding> validate(ValidationContext context) {
        List<ValidationFinding> findings = new ArrayList<>();
        for (BillingCase c : context.getCases()) {
            if (c.getBeneficiary() == null) {
                continue;
            }
            boolean exists = supportActivityRepository.existsByBeneficiaryIdAndBillingMonth(
                    c.getBeneficiary().getId(), context.getBillingMonth());
            if (!exists) {
                findings.add(new ValidationFinding(code(), ValidationSeverity.ERROR,
                        "No support activity for billing month", "activity", c));
            }
        }
        return findings;
    }
}
