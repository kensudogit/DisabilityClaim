package com.disabilityclaim.calculation.strategy;

import com.disabilityclaim.calculation.CalculationContext;
import com.disabilityclaim.domain.entity.BillingCaseItem;
import com.disabilityclaim.domain.entity.FeeRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BaseFeeRuleStrategy implements CalculationRuleStrategy {

    @Override
    public String ruleType() {
        return "BASE";
    }

    @Override
    public List<BillingCaseItem> apply(CalculationContext context) {
        List<BillingCaseItem> items = new ArrayList<>();
        List<FeeRule> baseRules = context.getFeeRules().stream()
                .filter(r -> "BASE".equals(r.getRuleType()))
                .toList();
        if (baseRules.isEmpty()) {
            context.markNeedsRuleData("No BASE fee rule found in FeeRuleSet for billing month");
            return items;
        }
        for (FeeRule rule : baseRules) {
            context.addStep("Apply BASE rule " + rule.getRuleCode() + " version=" + rule.getSourceVersion());
            if (rule.getUnits() == null && rule.getAmount() == null) {
                context.markNeedsRuleData("BASE rule " + rule.getRuleCode()
                        + " has NULL units/amount (source_document=" + rule.getSourceDocument() + ")");
                items.add(BillingCaseItem.builder()
                        .billingCase(context.getBillingCase())
                        .itemType("BASE")
                        .itemName(rule.getDescription())
                        .ruleCode(rule.getRuleCode())
                        .ruleVersion(rule.getSourceVersion())
                        .sortOrder(10)
                        .build());
                continue;
            }
            items.add(BillingCaseItem.builder()
                    .billingCase(context.getBillingCase())
                    .itemType("BASE")
                    .itemName(rule.getDescription())
                    .units(rule.getUnits())
                    .amount(rule.getAmount())
                    .ruleCode(rule.getRuleCode())
                    .ruleVersion(rule.getSourceVersion())
                    .sortOrder(10)
                    .build());
            if (rule.getUnits() != null) {
                int current = context.getTotalUnits() == null ? 0 : context.getTotalUnits();
                context.setTotalUnits(current + rule.getUnits());
            }
        }
        return items;
    }
}
