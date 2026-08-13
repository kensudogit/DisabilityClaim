package com.disabilityclaim.calculation.strategy;

import com.disabilityclaim.calculation.CalculationContext;
import com.disabilityclaim.domain.entity.BillingCaseItem;
import com.disabilityclaim.domain.entity.FeeRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Case-load reduction thresholds MUST come from masters — never hardcoded.
 */
@Component
public class CaseLoadReductionRuleStrategy implements CalculationRuleStrategy {

    @Override
    public String ruleType() {
        return "CASE_LOAD_REDUCTION";
    }

    @Override
    public List<BillingCaseItem> apply(CalculationContext context) {
        List<BillingCaseItem> items = new ArrayList<>();
        List<FeeRule> rules = context.getFeeRules().stream()
                .filter(r -> "CASE_LOAD_REDUCTION".equals(r.getRuleType()))
                .toList();
        Integer caseLoad = context.getCaseLoadCount();
        context.addStep("Case load count input=" + caseLoad);
        if (rules.isEmpty()) {
            context.addStep("No CASE_LOAD_REDUCTION rules loaded; threshold not applied");
            return items;
        }
        for (FeeRule rule : rules) {
            context.addStep("Evaluate CASE_LOAD_REDUCTION " + rule.getRuleCode()
                    + " condition_json=" + rule.getConditionJson());
            if (rule.getUnits() == null && rule.getAmount() == null
                    && (rule.getConditionJson() == null || rule.getConditionJson().isBlank())) {
                context.markNeedsRuleData("CASE_LOAD_REDUCTION rule " + rule.getRuleCode()
                        + " has no threshold/units (source=" + rule.getSourceDocument() + ")");
            }
            // Without official threshold values we do not invent reductions.
        }
        return items;
    }
}
