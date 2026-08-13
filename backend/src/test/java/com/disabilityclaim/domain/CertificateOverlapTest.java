package com.disabilityclaim.domain;

import com.disabilityclaim.domain.enums.ServiceCategory;
import com.disabilityclaim.service.BeneficiaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import com.disabilityclaim.domain.entity.RecipientCertificate;
import com.disabilityclaim.repository.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateOverlapTest {

    @Mock
    private BeneficiaryRepository beneficiaryRepository;
    @Mock
    private RecipientCertificateRepository certificateRepository;
    @Mock
    private OfficeProfileRepository officeProfileRepository;
    @Mock
    private MunicipalityRepository municipalityRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private com.disabilityclaim.service.AuditService auditService;

    @InjectMocks
    private BeneficiaryService beneficiaryService;

    @Test
    void overlappingCertificatePeriodsAreRejected() {
        UUID beneficiaryId = UUID.randomUUID();
        when(certificateRepository.findOverlapping(
                eq(beneficiaryId),
                eq(ServiceCategory.PLAN_CONSULTATION),
                eq(LocalDate.of(2026, 4, 1)),
                eq(LocalDate.of(2026, 9, 30)),
                isNull()))
                .thenReturn(List.of(RecipientCertificate.builder().id(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> beneficiaryService.assertNoOverlap(
                beneficiaryId,
                ServiceCategory.PLAN_CONSULTATION,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 9, 30),
                null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("overlaps");
    }
}
