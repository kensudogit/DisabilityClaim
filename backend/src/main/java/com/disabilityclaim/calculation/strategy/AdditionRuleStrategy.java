package com.disabilityclaim.calculation.strategy;

import com.disabilityclaim.calculation.CalculationContext;
import com.disabilityclaim.domain.entity.BillingCaseItem;
import com.disabilityclaim.domain.entity.FeeRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AdditionRuleStrategy implements CalculationRuleStrategy {

    @Override
    public String ruleType() {
        return "ADDITION";
    }

    @Override
    public List<BillingCaseItem> apply(CalculationContext context) {
        List<BillingCaseItem> items = new ArrayList<>();
        List<FeeRule> rules = context.getFeeRules().stream()
                .filter(r -> "ADDITION".equals(r.getRuleType()))
                .toList();
        if (rules.isEmpty()) {
            context.addStep("No ADDITION rules in FeeRuleSet (skipped)");
            return items;
        }
        for (FeeRule rule : rules) {
            context.addStep("Evaluate ADDITION " + rule.getRuleCode());
            if (rule.getUnits() == null && rule.getAmount() == null) {
                context.markNeedsRuleData("ADDITION rule " + rule.getRuleCode()
                        + " missing units/amount (source=" + rule.getSourceDocument() + ")");
                continue;
            }
            // Auto application requires official condition_json; without it, do not invent amounts.
            context.addStep("ADDITION " + rule.getRuleCode()
                    + " present but auto-apply conditions require official spec; recorded as pending confirm");
            items.add(BillingCaseItem.builder()
                    .billingCase(context.getBillingCase())
                    .itemType("ADDITION")
                    .itemName(rule.getDescription())
                    .units(rule.getUnits())
                    .amount(rule.getAmount())
                    .ruleCode(rule.getRuleCode())
                    .ruleVersion(rule.getSourceVersion())
                    .sortOrder(20)
                    .build());
        }
        return items;
    }
}
