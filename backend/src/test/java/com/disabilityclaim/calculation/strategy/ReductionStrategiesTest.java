package com.disabilityclaim.calculation.strategy;

import com.disabilityclaim.calculation.CalculationContext;
import com.disabilityclaim.domain.entity.BillingCase;
import com.disabilityclaim.domain.entity.FeeRule;
import com.disabilityclaim.domain.entity.FeeRuleSet;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReductionStrategiesTest {

    private CalculationContext context(List<FeeRule> rules, Integer caseLoad) {
        return CalculationContext.builder()
                .billingMonth("2026-08")
                .billingCase(BillingCase.builder().id(UUID.randomUUID()).build())
                .feeRuleSet(FeeRuleSet.builder()
                        .id(UUID.randomUUID())
                        .code("T")
                        .name("t")
                        .effectiveFrom(LocalDate.of(2024, 4, 1))
                        .sourceDocument("PENDING_OFFICIAL_SPEC")
                        .sourceVersion("PENDING")
                        .build())
                .feeRules(rules)
                .activities(List.of())
                .caseLoadCount(caseLoad)
                .build();
    }

    @Test
    void reductionSkipsWhenEmpty() {
        CalculationContext ctx = context(List.of(), null);
        assertThat(new ReductionRuleStrategy().apply(ctx)).isEmpty();
        assertThat(ctx.getSteps()).anyMatch(s -> s.contains("No REDUCTION"));
    }

    @Test
    void reductionMarksNeedsRuleDataWhenNullAmounts() {
        FeeRule rule = FeeRule.builder()
                .id(UUID.randomUUID())
                .ruleCode("RED_1")
                .ruleType("REDUCTION")
                .units(null)
                .amount(null)
                .sourceDocument("PENDING_OFFICIAL_SPEC")
                .sourceVersion("PENDING")
                .effectiveFrom(LocalDate.of(2024, 4, 1))
                .build();
        CalculationContext ctx = context(List.of(rule), null);
        new ReductionRuleStrategy().apply(ctx);
        assertThat(ctx.isNeedsRuleData()).isTrue();
    }

    @Test
    void caseLoadWithoutRulesDoesNotInventReduction() {
        CalculationContext ctx = context(List.of(), 40);
        assertThat(new CaseLoadReductionRuleStrategy().apply(ctx)).isEmpty();
        assertThat(ctx.isNeedsRuleData()).isFalse();
        assertThat(ctx.getSteps()).anyMatch(s -> s.contains("threshold not applied"));
    }

    @Test
    void caseLoadWithEmptyRuleMarksNeedsRuleData() {
        FeeRule rule = FeeRule.builder()
                .id(UUID.randomUUID())
                .ruleCode("CLR_1")
                .ruleType("CASE_LOAD_REDUCTION")
                .units(null)
                .amount(null)
                .conditionJson(null)
                .sourceDocument("PENDING_OFFICIAL_SPEC")
                .sourceVersion("PENDING")
                .effectiveFrom(LocalDate.of(2024, 4, 1))
                .build();
        CalculationContext ctx = context(List.of(rule), 40);
        new CaseLoadReductionRuleStrategy().apply(ctx);
        assertThat(ctx.isNeedsRuleData()).isTrue();
    }
}
