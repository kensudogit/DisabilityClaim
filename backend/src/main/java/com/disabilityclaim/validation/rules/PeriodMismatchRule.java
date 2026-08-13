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
public class PeriodMismatchRule implements ValidationRule {
    @Override
    public String code() {
        return "PERIOD_MISMATCH";
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
            boolean overlaps = !cert.getValidFrom().isAfter(monthEnd) && !cert.getValidTo().isBefore(monthStart);
            if (!overlaps) {
                findings.add(new ValidationFinding(code(), ValidationSeverity.ERROR,
                        "Certificate period does not overlap billing month", "certificate.period", c));
            }
        }
        return findings;
    }
}
