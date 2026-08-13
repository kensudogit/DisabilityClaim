package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.FeeRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeeRuleRepository extends JpaRepository<FeeRule, UUID> {
    List<FeeRule> findByRuleSetId(UUID ruleSetId);

    List<FeeRule> findByRuleSetIdAndRuleType(UUID ruleSetId, String ruleType);
}
