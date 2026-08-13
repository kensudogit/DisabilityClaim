package com.disabilityclaim.validation.rules;

import com.disabilityclaim.domain.entity.BillingCase;
import com.disabilityclaim.domain.enums.BeneficiaryCategory;
import com.disabilityclaim.domain.enums.ServiceCategory;
import com.disabilityclaim.domain.enums.ValidationSeverity;
import com.disabilityclaim.validation.ValidationContext;
import com.disabilityclaim.validation.ValidationFinding;
import com.disabilityclaim.validation.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ServiceCategoryMismatchRule implements ValidationRule {
    @Override
    public String code() {
        return "SERVICE_CATEGORY_MISMATCH";
    }

    @Override
    public List<ValidationFinding> validate(ValidationContext context) {
        List<ValidationFinding> findings = new ArrayList<>();
        for (BillingCase c : context.getCases()) {
            if (c.getCategory() == null || c.getServiceCategory() == null) {
                continue;
            }
            boolean childCategory = c.getCategory() == BeneficiaryCategory.CHILD;
            boolean childService = c.getServiceCategory() == ServiceCategory.CHILD_CONSULTATION;
            boolean adultService = c.getServiceCategory() == ServiceCategory.PLAN_CONSULTATION
                    || c.getServiceCategory() == ServiceCategory.MONITORING;
            if (childCategory && adultService && !childService) {
                findings.add(new ValidationFinding(code(), ValidationSeverity.ERROR,
                        "Child beneficiary with adult plan consultation category", "serviceCategory", c));
            }
            if (!childCategory && childService) {
                findings.add(new ValidationFinding(code(), ValidationSeverity.ERROR,
                        "Adult beneficiary with child consultation category", "serviceCategory", c));
            }
        }
        return findings;
    }
}
