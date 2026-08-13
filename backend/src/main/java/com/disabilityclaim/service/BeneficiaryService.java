package com.disabilityclaim.service;

import com.disabilityclaim.domain.entity.*;
import com.disabilityclaim.domain.enums.BeneficiaryStatus;
import com.disabilityclaim.domain.enums.ServiceCategory;
import com.disabilityclaim.repository.*;
import com.disabilityclaim.web.dto.BeneficiaryDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final RecipientCertificateRepository certificateRepository;
    private final OfficeProfileRepository officeProfileRepository;
    private final MunicipalityRepository municipalityRepository;
    private final StaffRepository staffRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<BeneficiaryDtos.BeneficiaryResponse> list() {
        return beneficiaryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BeneficiaryDtos.BeneficiaryResponse get(UUID id) {
        return toResponse(findBeneficiary(id));
    }

    @Transactional
    public BeneficiaryDtos.BeneficiaryResponse create(BeneficiaryDtos.BeneficiaryRequest request, UUID actorId, String actorName) {
        OfficeProfile office = officeProfileRepository.findById(request.officeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "office not found"));
        Beneficiary beneficiary = Beneficiary.builder()
                .office(office)
                .category(request.category())
                .recipientNumber(request.recipientNumber())
                .familyName(request.familyName())
                .givenName(request.givenName())
                .familyNameKana(request.familyNameKana())
                .givenNameKana(request.givenNameKana())
                .birthDate(request.birthDate())
                .status(request.status() != null ? request.status() : BeneficiaryStatus.ACTIVE)
                .serviceStartDate(request.serviceStartDate())
                .serviceEndDate(request.serviceEndDate())
                .notes(request.notes())
                .build();
        applyRefs(beneficiary, request);
        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        auditService.record(actorId, actorName, "CREATE", "Beneficiary", saved.getId().toString(), null, saved.getFamilyName(), null);
        return toResponse(saved);
    }

    @Transactional
    public BeneficiaryDtos.BeneficiaryResponse update(UUID id, BeneficiaryDtos.BeneficiaryRequest request, UUID actorId, String actorName) {
        Beneficiary beneficiary = findBeneficiary(id);
        String before = beneficiary.getFamilyName();
        beneficiary.setCategory(request.category());
        beneficiary.setRecipientNumber(request.recipientNumber());
        beneficiary.setFamilyName(request.familyName());
        beneficiary.setGivenName(request.givenName());
        beneficiary.setFamilyNameKana(request.familyNameKana());
        beneficiary.setGivenNameKana(request.givenNameKana());
        beneficiary.setBirthDate(request.birthDate());
        if (request.status() != null) {
            beneficiary.setStatus(request.status());
        }
        beneficiary.setServiceStartDate(request.serviceStartDate());
        beneficiary.setServiceEndDate(request.serviceEndDate());
        beneficiary.setNotes(request.notes());
        applyRefs(beneficiary, request);
        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        auditService.record(actorId, actorName, "UPDATE", "Beneficiary", id.toString(), before, saved.getFamilyName(), null);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryDtos.CertificateResponse> listCertificates(UUID beneficiaryId) {
        findBeneficiary(beneficiaryId);
        return certificateRepository.findByBeneficiaryId(beneficiaryId).stream().map(this::toCertResponse).toList();
    }

    @Transactional
    public BeneficiaryDtos.CertificateResponse addCertificate(
            UUID beneficiaryId, BeneficiaryDtos.CertificateRequest request, UUID actorId, String actorName) {
        Beneficiary beneficiary = findBeneficiary(beneficiaryId);
        assertNoOverlap(beneficiaryId, request.serviceCategory(), request.validFrom(), request.validTo(), null);
        Municipality municipality = municipalityRepository.findById(request.municipalityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "municipality not found"));
        if (request.validTo().isBefore(request.validFrom())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "validTo must be >= validFrom");
        }
        RecipientCertificate cert = RecipientCertificate.builder()
                .beneficiary(beneficiary)
                .certificateNumber(request.certificateNumber())
                .municipality(municipality)
                .validFrom(request.validFrom())
                .validTo(request.validTo())
                .serviceCategory(request.serviceCategory())
                .monitoringMonths(request.monitoringMonths())
                .notes(request.notes())
                .build();
        RecipientCertificate saved = certificateRepository.save(cert);
        auditService.record(actorId, actorName, "CREATE", "RecipientCertificate", saved.getId().toString(), null, saved.getCertificateNumber(), null);
        return toCertResponse(saved);
    }

    public void assertNoOverlap(UUID beneficiaryId, ServiceCategory category,
                                java.time.LocalDate from, java.time.LocalDate to, UUID excludeId) {
        List<RecipientCertificate> overlaps = certificateRepository.findOverlapping(
                beneficiaryId, category, from, to, excludeId);
        if (!overlaps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Certificate period overlaps existing certificate for same service category");
        }
    }

    private void applyRefs(Beneficiary beneficiary, BeneficiaryDtos.BeneficiaryRequest request) {
        if (request.municipalityId() != null) {
            beneficiary.setMunicipality(municipalityRepository.findById(request.municipalityId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "municipality not found")));
        } else {
            beneficiary.setMunicipality(null);
        }
        if (request.primaryStaffId() != null) {
            beneficiary.setPrimaryStaff(staffRepository.findById(request.primaryStaffId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "staff not found")));
        } else {
            beneficiary.setPrimaryStaff(null);
        }
    }

    private Beneficiary findBeneficiary(UUID id) {
        return beneficiaryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "beneficiary not found"));
    }

    private BeneficiaryDtos.BeneficiaryResponse toResponse(Beneficiary b) {
        return new BeneficiaryDtos.BeneficiaryResponse(
                b.getId(),
                b.getOffice().getId(),
                b.getCategory(),
                b.getRecipientNumber(),
                b.getFamilyName(),
                b.getGivenName(),
                b.getStatus(),
                b.getMunicipality() != null ? b.getMunicipality().getId() : null,
                b.getPrimaryStaff() != null ? b.getPrimaryStaff().getId() : null
        );
    }

    private BeneficiaryDtos.CertificateResponse toCertResponse(RecipientCertificate c) {
        return new BeneficiaryDtos.CertificateResponse(
                c.getId(),
                c.getBeneficiary().getId(),
                c.getCertificateNumber(),
                c.getMunicipality().getId(),
                c.getValidFrom(),
                c.getValidTo(),
                c.getServiceCategory(),
                c.getMonitoringMonths()
        );
    }
}
