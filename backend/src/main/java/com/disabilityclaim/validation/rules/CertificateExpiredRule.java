package com.disabilityclaim.validation.rules;

import com.disabilityclaim.domain.entity.BillingCase;
import com.disabilityclaim.domain.entity.RecipientCertificate;
import com.disabilityclaim.domain.enums.ValidationSeverity;
import com.disabilityclaim.validation.ValidationContext;
import com.disabilityclaim.validation.ValidationFinding;
import com.disabilityclaim.validation.ValidationRule;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Component
public class CertificateExpiredRule implements ValidationRule {
    @Override
    public String code() {
        return "CERTIFICATE_EXPIRED";
    }

    @Override
    public List<ValidationFinding> validate(ValidationContext context) {
        List<ValidationFinding> findings = new ArrayList<>();
        YearMonth ym = YearMonth.parse(context.getBillingMonth());
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();
        for (BillingCase c : context.getCases()) {
            RecipientCertificate cert = c.getCertificate();
            if (cert == null) {
                continue;
            }
            if (cert.getValidTo().isBefore(monthStart) || cert.getValidFrom().isAfter(monthEnd)) {
                findings.add(new ValidationFinding(code(), ValidationSeverity.ERROR,
                        "Certificate not valid in billing month " + context.getBillingMonth(),
                        "certificate.validTo", c));
            }
        }
        return findings;
    }
}
