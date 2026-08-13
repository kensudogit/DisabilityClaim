package com.disabilityclaim.calculation.strategy;

import com.disabilityclaim.calculation.CalculationContext;
import com.disabilityclaim.domain.entity.BillingCase;
import com.disabilityclaim.domain.entity.BillingCaseItem;
import com.disabilityclaim.domain.entity.FeeRule;
import com.disabilityclaim.domain.entity.FeeRuleSet;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CalculationStrategiesTest {

    private CalculationContext context(List<FeeRule> rules) {
        BillingCase billingCase = BillingCase.builder().id(UUID.randomUUID()).build();
        FeeRuleSet set = FeeRuleSet.builder()
                .id(UUID.randomUUID())
                .code("TEST")
                .name("test")
                .effectiveFrom(LocalDate.of(2024, 4, 1))
                .sourceDocument("TEST_SPEC")
                .sourceVersion("1")
                .build();
        return CalculationContext.builder()
                .billingMonth("2026-08")
                .billingCase(billingCase)
                .feeRuleSet(set)
                .feeRules(rules)
                .activities(List.of())
                .build();
    }

    @Test
    void baseStrategyMarksNeedsRuleDataWhenNoBaseRule() {
        CalculationContext ctx = context(List.of());
        List<BillingCaseItem> items = new BaseFeeRuleStrategy().apply(ctx);
        assertThat(items).isEmpty();
        assertThat(ctx.isNeedsRuleData()).isTrue();
        assertThat(ctx.getNeedsRuleDataReason()).contains("No BASE");
    }

    @Test
    void baseStrategyAppliesUnitsWhenPresent() {
        FeeRule rule = FeeRule.builder()
                .id(UUID.randomUUID())
                .ruleCode("BASE_001")
                .ruleType("BASE")
                .description("基本報酬")
                .units(500)
                .amount(new BigDecimal("10000"))
                .sourceVersion("1")
                .sourceDocument("TEST_SPEC")
                .effectiveFrom(LocalDate.of(2024, 4, 1))
                .build();
        CalculationContext ctx = context(List.of(rule));
        List<BillingCaseItem> items = new BaseFeeRuleStrategy().apply(ctx);
        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getUnits()).isEqualTo(500);
        assertThat(ctx.getTotalUnits()).isEqualTo(500);
        assertThat(ctx.isNeedsRuleData()).isFalse();
    }

    @Test
    void baseStrategyMarksNeedsRuleDataWhenUnitsAndAmountNull() {
        FeeRule rule = FeeRule.builder()
                .id(UUID.randomUUID())
                .ruleCode("BASE_PENDING")
                .ruleType("BASE")
                .units(null)
                .amount(null)
                .sourceDocument("PENDING_OFFICIAL_SPEC")
                .sourceVersion("PENDING")
                .effectiveFrom(LocalDate.of(2024, 4, 1))
                .build();
        CalculationContext ctx = context(List.of(rule));
        List<BillingCaseItem> items = new BaseFeeRuleStrategy().apply(ctx);
        assertThat(items).hasSize(1);
        assertThat(ctx.isNeedsRuleData()).isTrue();
    }

    @Test
    void additionStrategySkipsWhenNoRules() {
        CalculationContext ctx = context(List.of());
        assertThat(new AdditionRuleStrategy().apply(ctx)).isEmpty();
        assertThat(ctx.getSteps()).anyMatch(s -> s.contains("No ADDITION"));
    }

    @Test
    void additionStrategyMarksNeedsRuleDataWhenNullAmounts() {
        FeeRule rule = FeeRule.builder()
                .id(UUID.randomUUID())
                .ruleCode("ADD_1")
                .ruleType("ADDITION")
                .units(null)
                .amount(null)
                .sourceDocument("PENDING_OFFICIAL_SPEC")
                .sourceVersion("PENDING")
                .effectiveFrom(LocalDate.of(2024, 4, 1))
                .build();
        CalculationContext ctx = context(List.of(rule));
        assertThat(new AdditionRuleStrategy().apply(ctx)).isEmpty();
        assertThat(ctx.isNeedsRuleData()).isTrue();
    }

    @Test
    void additionStrategyRecordsItemWhenUnitsPresent() {
        FeeRule rule = FeeRule.builder()
                .id(UUID.randomUUID())
                .ruleCode("ADD_2")
                .ruleType("ADDITION")
                .description("加算")
                .units(50)
                .amount(new BigDecimal("1000"))
                .sourceDocument("TEST_SPEC")
                .sourceVersion("1")
                .effectiveFrom(LocalDate.of(2024, 4, 1))
                .build();
        CalculationContext ctx = context(List.of(rule));
        List<BillingCaseItem> items = new AdditionRuleStrategy().apply(ctx);
        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getItemType()).isEqualTo("ADDITION");
    }

    @Test
    void reductionAndCaseLoadStrategiesHaveExpectedRuleTypes() {
        assertThat(new ReductionRuleStrategy().ruleType()).isEqualTo("REDUCTION");
        assertThat(new CaseLoadReductionRuleStrategy().ruleType()).isEqualTo("CASE_LOAD_REDUCTION");
        assertThat(new BaseFeeRuleStrategy().ruleType()).isEqualTo("BASE");
        assertThat(new AdditionRuleStrategy().ruleType()).isEqualTo("ADDITION");
    }
}
