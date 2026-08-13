package com.disabilityclaim.validation;

import com.disabilityclaim.domain.entity.BillingBatch;
import com.disabilityclaim.domain.entity.BillingCase;
import com.disabilityclaim.domain.entity.BillingValidation;
import com.disabilityclaim.repository.BillingValidationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ValidationEngine {

    private final List<ValidationRule> rules;
    private final BillingValidationRepository validationRepository;

    @Transactional
    public List<BillingValidation> validate(BillingBatch batch, List<BillingCase> cases) {
        validationRepository.deleteByBatchId(batch.getId());
        ValidationContext context = ValidationContext.builder()
                .batch(batch)
                .cases(cases)
                .billingMonth(batch.getBillingMonth())
                .build();

        List<BillingValidation> saved = new ArrayList<>();
        for (ValidationRule rule : rules) {
            for (ValidationFinding finding : rule.validate(context)) {
                BillingValidation v = BillingValidation.builder()
                        .batch(batch)
                        .billingCase(finding.billingCase())
                        .ruleCode(finding.ruleCode())
                        .severity(finding.severity())
                        .message(finding.message())
                        .fieldName(finding.fieldName())
                        .build();
                saved.add(validationRepository.save(v));
            }
        }
        return saved;
    }

    public List<ValidationFinding> validateInMemory(ValidationContext context) {
        List<ValidationFinding> all = new ArrayList<>();
        for (ValidationRule rule : rules) {
            all.addAll(rule.validate(context));
        }
        return all;
    }
}
