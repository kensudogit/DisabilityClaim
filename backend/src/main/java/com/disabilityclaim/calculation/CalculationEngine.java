package com.disabilityclaim.calculation;

import com.disabilityclaim.calculation.strategy.CalculationRuleStrategy;
import com.disabilityclaim.domain.entity.*;
import com.disabilityclaim.domain.enums.BillingCaseStatus;
import com.disabilityclaim.repository.BillingCalculationTraceRepository;
import com.disabilityclaim.repository.BillingCaseItemRepository;
import com.disabilityclaim.repository.FeeRuleRepository;
import com.disabilityclaim.repository.FeeRuleSetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CalculationEngine {

    private final FeeRuleSetRepository feeRuleSetRepository;
    private final FeeRuleRepository feeRuleRepository;
    private final BillingCaseItemRepository caseItemRepository;
    private final BillingCalculationTraceRepository traceRepository;
    private final List<CalculationRuleStrategy> strategies;
    private final ObjectMapper objectMapper;

    @Transactional
    public BillingCase calculate(BillingCase billingCase, String billingMonth, Integer caseLoadCount, String regionCategoryCode) {
        LocalDate onDate = YearMonth.parse(billingMonth).atEndOfMonth();
        List<FeeRuleSet> ruleSets = feeRuleSetRepository.findEffectiveOn(onDate);
        FeeRuleSet ruleSet = ruleSets.isEmpty() ? null : ruleSets.getFirst();

        List<FeeRule> rules = ruleSet == null ? List.of() : feeRuleRepository.findByRuleSetId(ruleSet.getId());

        CalculationContext context = CalculationContext.builder()
                .billingMonth(billingMonth)
                .billingCase(billingCase)
                .feeRuleSet(ruleSet)
                .feeRules(rules)
                .activities(List.of())
                .caseLoadCount(caseLoadCount)
                .regionCategoryCode(regionCategoryCode)
                .build();

        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("billingMonth", billingMonth);
        inputs.put("beneficiaryId", billingCase.getBeneficiary().getId());
        inputs.put("caseLoadCount", caseLoadCount);
        inputs.put("regionCategoryCode", regionCategoryCode);
        inputs.put("ruleSetCode", ruleSet != null ? ruleSet.getCode() : null);
        inputs.put("ruleSetVersion", ruleSet != null ? ruleSet.getSourceVersion() : null);
        inputs.put("sourceDocument", ruleSet != null ? ruleSet.getSourceDocument() : "NONE");

        if (ruleSet == null) {
            context.markNeedsRuleData("No FeeRuleSet effective for " + billingMonth);
        } else if ("PENDING_OFFICIAL_SPEC".equals(ruleSet.getSourceDocument())) {
            context.markNeedsRuleData("FeeRuleSet source_document=PENDING_OFFICIAL_SPEC; official fee tables not loaded");
        }

        caseItemRepository.deleteByBillingCaseId(billingCase.getId());
        List<BillingCaseItem> allItems = new ArrayList<>();
        for (CalculationRuleStrategy strategy : strategies) {
            context.addStep("Run strategy " + strategy.ruleType());
            allItems.addAll(strategy.apply(context));
        }
        caseItemRepository.saveAll(allItems);

        billingCase.setTotalUnits(context.getTotalUnits());
        billingCase.setBilledAmount(context.getBilledAmount());
        if (context.isNeedsRuleData()) {
            billingCase.setStatus(BillingCaseStatus.NEEDS_RULE_DATA);
        } else {
            billingCase.setStatus(BillingCaseStatus.CALCULATED);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", billingCase.getStatus().name());
        result.put("totalUnits", context.getTotalUnits());
        result.put("billedAmount", context.getBilledAmount());
        result.put("needsRuleData", context.isNeedsRuleData());
        result.put("needsRuleDataReason", context.getNeedsRuleDataReason());
        result.put("itemCount", allItems.size());

        traceRepository.save(BillingCalculationTrace.builder()
                .billingCase(billingCase)
                .ruleSetCode(ruleSet != null ? ruleSet.getCode() : null)
                .ruleVersion(ruleSet != null ? ruleSet.getSourceVersion() : null)
                .sourceDocument(ruleSet != null ? ruleSet.getSourceDocument() : null)
                .inputsJson(toJson(inputs))
                .stepsJson(toJson(context.getSteps()))
                .resultJson(toJson(result))
                .build());

        return billingCase;
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }
}
