package com.disabilityclaim.calculation.strategy;

import com.disabilityclaim.calculation.CalculationContext;
import com.disabilityclaim.domain.entity.BillingCaseItem;
import com.disabilityclaim.domain.entity.FeeRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ReductionRuleStrategy implements CalculationRuleStrategy {

    @Override
    public String ruleType() {
        return "REDUCTION";
    }

    @Override
    public List<BillingCaseItem> apply(CalculationContext context) {
        List<BillingCaseItem> items = new ArrayList<>();
        List<FeeRule> rules = context.getFeeRules().stream()
                .filter(r -> "REDUCTION".equals(r.getRuleType()))
                .toList();
        if (rules.isEmpty()) {
            context.addStep("No REDUCTION rules in FeeRuleSet (skipped)");
            return items;
        }
        for (FeeRule rule : rules) {
            context.addStep("Evaluate REDUCTION " + rule.getRuleCode());
            if (rule.getUnits() == null && rule.getAmount() == null) {
                context.markNeedsRuleData("REDUCTION rule " + rule.getRuleCode()
                        + " missing units/amount (source=" + rule.getSourceDocument() + ")");
            }
        }
        return items;
    }
}
