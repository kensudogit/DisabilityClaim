package com.disabilityclaim.calculation;

import com.disabilityclaim.calculation.strategy.*;
import com.disabilityclaim.domain.entity.*;
import com.disabilityclaim.domain.enums.BillingCaseStatus;
import com.disabilityclaim.repository.BillingCalculationTraceRepository;
import com.disabilityclaim.repository.BillingCaseItemRepository;
import com.disabilityclaim.repository.FeeRuleRepository;
import com.disabilityclaim.repository.FeeRuleSetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculationEngineTest {

    @Mock
    private FeeRuleSetRepository feeRuleSetRepository;
    @Mock
    private FeeRuleRepository feeRuleRepository;
    @Mock
    private BillingCaseItemRepository caseItemRepository;
    @Mock
    private BillingCalculationTraceRepository traceRepository;

    private CalculationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new CalculationEngine(
                feeRuleSetRepository,
                feeRuleRepository,
                caseItemRepository,
                traceRepository,
                List.of(
                        new BaseFeeRuleStrategy(),
                        new AdditionRuleStrategy(),
                        new ReductionRuleStrategy(),
                        new CaseLoadReductionRuleStrategy()),
                new ObjectMapper());
    }

    @Test
    void emptyOrPendingMastersMarkNeedsRuleDataAndWriteTrace() {
        FeeRuleSet pending = FeeRuleSet.builder()
                .id(UUID.randomUUID())
                .code("PLACEHOLDER")
                .name("pending")
                .effectiveFrom(LocalDate.of(2024, 4, 1))
                .sourceDocument("PENDING_OFFICIAL_SPEC")
                .sourceVersion("PENDING")
                .build();
        FeeRule base = FeeRule.builder()
                .id(UUID.randomUUID())
                .ruleSet(pending)
                .ruleCode("BASE_PLACEHOLDER")
                .ruleType("BASE")
                .units(null)
                .amount(null)
                .effectiveFrom(LocalDate.of(2024, 4, 1))
                .sourceDocument("PENDING_OFFICIAL_SPEC")
                .sourceVersion("PENDING")
                .build();

        when(feeRuleSetRepository.findEffectiveOn(any())).thenReturn(List.of(pending));
        when(feeRuleRepository.findByRuleSetId(pending.getId())).thenReturn(List.of(base));

        BillingCase billingCase = BillingCase.builder()
                .id(UUID.randomUUID())
                .beneficiary(Beneficiary.builder().id(UUID.randomUUID()).build())
                .status(BillingCaseStatus.CANDIDATE)
                .build();

        BillingCase result = engine.calculate(billingCase, "2026-08", 10, "PENDING_REGION");

        assertThat(result.getStatus()).isEqualTo(BillingCaseStatus.NEEDS_RULE_DATA);

        ArgumentCaptor<BillingCalculationTrace> captor = ArgumentCaptor.forClass(BillingCalculationTrace.class);
        verify(traceRepository).save(captor.capture());
        BillingCalculationTrace trace = captor.getValue();
        assertThat(trace.getSourceDocument()).isEqualTo("PENDING_OFFICIAL_SPEC");
        assertThat(trace.getInputsJson()).contains("2026-08");
        assertThat(trace.getStepsJson()).contains("NEEDS_RULE_DATA");
        assertThat(trace.getResultJson()).contains("NEEDS_RULE_DATA");
    }

    @Test
    void missingRuleSetMarksNeedsRuleData() {
        when(feeRuleSetRepository.findEffectiveOn(any())).thenReturn(List.of());
        BillingCase billingCase = BillingCase.builder()
                .id(UUID.randomUUID())
                .beneficiary(Beneficiary.builder().id(UUID.randomUUID()).build())
                .status(BillingCaseStatus.CANDIDATE)
                .build();

        BillingCase result = engine.calculate(billingCase, "2026-08", null, null);
        assertThat(result.getStatus()).isEqualTo(BillingCaseStatus.NEEDS_RULE_DATA);
        verify(traceRepository).save(any(BillingCalculationTrace.class));
    }
}
