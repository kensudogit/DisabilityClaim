package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.Municipality;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MunicipalityRepository extends JpaRepository<Municipality, UUID> {
    Optional<Municipality> findByCode(String code);
}
