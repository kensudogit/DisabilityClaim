package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.CertificateServiceDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CertificateServiceDetailRepository extends JpaRepository<CertificateServiceDetail, UUID> {
    List<CertificateServiceDetail> findByCertificateId(UUID certificateId);
}
