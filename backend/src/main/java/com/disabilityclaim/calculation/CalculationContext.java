package com.disabilityclaim.calculation;

import com.disabilityclaim.domain.entity.BillingCase;
import com.disabilityclaim.domain.entity.FeeRule;
import com.disabilityclaim.domain.entity.FeeRuleSet;
import com.disabilityclaim.domain.entity.SupportActivity;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class CalculationContext {
    private final String billingMonth;
    private final BillingCase billingCase;
    private final FeeRuleSet feeRuleSet;
    private final List<FeeRule> feeRules;
    private final List<SupportActivity> activities;
    private final Integer caseLoadCount;
    private final String regionCategoryCode;

    @Builder.Default
    private final List<String> steps = new ArrayList<>();

    private Integer totalUnits;
    private BigDecimal billedAmount;
    private boolean needsRuleData;
    private String needsRuleDataReason;

    public void addStep(String step) {
        steps.add(step);
    }

    public void markNeedsRuleData(String reason) {
        this.needsRuleData = true;
        this.needsRuleDataReason = reason;
        addStep("NEEDS_RULE_DATA: " + reason);
    }

    public void setTotalUnits(Integer totalUnits) {
        this.totalUnits = totalUnits;
    }

    public void setBilledAmount(BigDecimal billedAmount) {
        this.billedAmount = billedAmount;
    }
}
