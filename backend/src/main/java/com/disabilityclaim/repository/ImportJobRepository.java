package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {
}
