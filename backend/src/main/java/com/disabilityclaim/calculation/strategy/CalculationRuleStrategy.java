package com.disabilityclaim.calculation.strategy;

import com.disabilityclaim.calculation.CalculationContext;
import com.disabilityclaim.domain.entity.BillingCaseItem;

import java.util.List;

public interface CalculationRuleStrategy {
    String ruleType();

    List<BillingCaseItem> apply(CalculationContext context);
}
