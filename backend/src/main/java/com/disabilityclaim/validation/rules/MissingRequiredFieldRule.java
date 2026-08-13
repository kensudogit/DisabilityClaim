package com.disabilityclaim.validation.rules;

import com.disabilityclaim.domain.entity.BillingCase;
import com.disabilityclaim.domain.enums.ValidationSeverity;
import com.disabilityclaim.validation.ValidationContext;
import com.disabilityclaim.validation.ValidationFinding;
import com.disabilityclaim.validation.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MissingRequiredFieldRule implements ValidationRule {
    @Override
    public String code() {
        return "MISSING_REQUIRED_FIELD";
    }

    @Override
    public List<ValidationFinding> validate(ValidationContext context) {
        List<ValidationFinding> findings = new ArrayList<>();
        for (BillingCase c : context.getCases()) {
            if (c.getMunicipality() == null) {
                findings.add(new ValidationFinding(code(), ValidationSeverity.ERROR,
                        "Municipality is required", "municipalityId", c));
            }
            if (c.getCertificate() == null) {
                findings.add(new ValidationFinding(code(), ValidationSeverity.ERROR,
                        "Certificate is required", "certificateId", c));
            }
            if (c.getBeneficiary() == null) {
                findings.add(new ValidationFinding(code(), ValidationSeverity.ERROR,
                        "Beneficiary is required", "beneficiaryId", c));
            }
        }
        return findings;
    }
}
