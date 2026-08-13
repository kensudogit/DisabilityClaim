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
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    public static final UUID DEMO_OFFICE_ID = UUID.fromString("33333333-3333-3333-3333-333333333001");

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
    public List<BeneficiaryDtos.BeneficiaryResponse> search(String q) {
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return beneficiaryRepository.findAll().stream()
                .map(this::toResponse)
                .filter(b -> needle.isEmpty() || matches(b, needle))
                .toList();
    }

    @Transactional(readOnly = true)
    public BeneficiaryDtos.BeneficiaryResponse get(UUID id) {
        return toResponse(findBeneficiary(id));
    }

    @Transactional
    public BeneficiaryDtos.BeneficiaryResponse create(BeneficiaryDtos.BeneficiaryRequest request, UUID actorId, String actorName) {
        UUID officeId = request.officeId() != null ? request.officeId() : DEMO_OFFICE_ID;
        OfficeProfile office = officeProfileRepository.findById(officeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "office not found"));

        String anonymized = resolveAnonymizedCode(request);
        String familyName = blankToNull(request.familyName()) != null ? request.familyName().trim() : anonymized;
        String givenName = blankToNull(request.givenName()) != null ? request.givenName().trim() : "匿名";

        Beneficiary beneficiary = Beneficiary.builder()
                .office(office)
                .category(request.category())
                .recipientNumber(firstNonBlank(request.recipientNumber(), anonymized))
                .familyName(familyName)
                .givenName(givenName)
                .familyNameKana(request.familyNameKana())
                .givenNameKana(request.givenNameKana())
                .birthDate(request.birthDate())
                .status(resolveStatus(request))
                .serviceStartDate(request.serviceStartDate())
                .serviceEndDate(request.serviceEndDate())
                .notes(firstNonBlank(request.notes(), "anonymizedCode=" + anonymized))
                .build();
        applyRefs(beneficiary, request);
        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        auditService.record(actorId, actorName, "CREATE", "Beneficiary", saved.getId().toString(), null, anonymized, null);
        return toResponse(saved);
    }

    @Transactional
    public BeneficiaryDtos.BeneficiaryResponse update(UUID id, BeneficiaryDtos.BeneficiaryRequest request, UUID actorId, String actorName) {
        Beneficiary beneficiary = findBeneficiary(id);
        String before = resolveAnonymizedFromEntity(beneficiary);
        String anonymized = resolveAnonymizedCode(request);
        beneficiary.setCategory(request.category());
        beneficiary.setRecipientNumber(firstNonBlank(request.recipientNumber(), anonymized));
        beneficiary.setFamilyName(blankToNull(request.familyName()) != null ? request.familyName().trim() : anonymized);
        beneficiary.setGivenName(blankToNull(request.givenName()) != null ? request.givenName().trim() : "匿名");
        beneficiary.setFamilyNameKana(request.familyNameKana());
        beneficiary.setGivenNameKana(request.givenNameKana());
        beneficiary.setBirthDate(request.birthDate());
        beneficiary.setStatus(resolveStatus(request));
        beneficiary.setServiceStartDate(request.serviceStartDate());
        beneficiary.setServiceEndDate(request.serviceEndDate());
        if (blankToNull(request.notes()) != null) {
            beneficiary.setNotes(request.notes());
        }
        applyRefs(beneficiary, request);
        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        auditService.record(actorId, actorName, "UPDATE", "Beneficiary", id.toString(), before, anonymized, null);
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
        Municipality municipality = resolveMunicipality(request.municipalityId(), request.municipalityCode());
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
        beneficiary.setMunicipality(resolveMunicipality(request.municipalityId(), request.municipalityCode()));
        if (request.primaryStaffId() != null) {
            beneficiary.setPrimaryStaff(staffRepository.findById(request.primaryStaffId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "staff not found")));
        } else {
            beneficiary.setPrimaryStaff(null);
        }
    }

    private Municipality resolveMunicipality(UUID municipalityId, String municipalityCode) {
        if (municipalityId != null) {
            return municipalityRepository.findById(municipalityId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "municipality not found"));
        }
        if (blankToNull(municipalityCode) != null) {
            return municipalityRepository.findByCode(municipalityCode.trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "municipality not found: " + municipalityCode));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "municipalityCode or municipalityId is required");
    }

    private Beneficiary findBeneficiary(UUID id) {
        return beneficiaryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "beneficiary not found"));
    }

    private BeneficiaryDtos.BeneficiaryResponse toResponse(Beneficiary b) {
        String anonymized = resolveAnonymizedFromEntity(b);
        return new BeneficiaryDtos.BeneficiaryResponse(
                b.getId(),
                b.getOffice().getId(),
                anonymized,
                b.getCategory(),
                b.getRecipientNumber(),
                b.getFamilyName(),
                b.getGivenName(),
                b.getStatus(),
                statusLabel(b.getStatus()),
                b.getMunicipality() != null ? b.getMunicipality().getId() : null,
                b.getMunicipality() != null ? b.getMunicipality().getCode() : null,
                b.getMunicipality() != null ? b.getMunicipality().getName() : null,
                b.getPrimaryStaff() != null ? b.getPrimaryStaff().getId() : null,
                b.getPrimaryStaff() != null ? b.getPrimaryStaff().getDisplayName() : null,
                b.getServiceStartDate(),
                b.getServiceEndDate()
        );
    }

    private BeneficiaryDtos.CertificateResponse toCertResponse(RecipientCertificate c) {
        Integer months = c.getMonitoringMonths();
        return new BeneficiaryDtos.CertificateResponse(
                c.getId(),
                c.getBeneficiary().getId(),
                c.getCertificateNumber(),
                c.getMunicipality().getId(),
                c.getMunicipality().getCode(),
                c.getMunicipality().getName(),
                c.getValidFrom(),
                c.getValidTo(),
                c.getServiceCategory(),
                months,
                months
        );
    }

    private static String resolveAnonymizedCode(BeneficiaryDtos.BeneficiaryRequest request) {
        String code = firstNonBlank(request.anonymizedCode(), request.recipientNumber(), request.familyName());
        if (code == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "anonymizedCode (or recipientNumber / familyName) is required");
        }
        return code.trim();
    }

    private static String resolveAnonymizedFromEntity(Beneficiary b) {
        return firstNonBlank(b.getRecipientNumber(), b.getFamilyName(), b.getId().toString());
    }

    private static BeneficiaryStatus resolveStatus(BeneficiaryDtos.BeneficiaryRequest request) {
        if (request.status() != null) {
            return request.status();
        }
        String raw = blankToNull(request.statusCode());
        if (raw == null) {
            return BeneficiaryStatus.ACTIVE;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if ("INACTIVE".equals(normalized)) {
            return BeneficiaryStatus.CLOSED;
        }
        try {
            return BeneficiaryStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid status: " + raw);
        }
    }

    private static String statusLabel(BeneficiaryStatus status) {
        return switch (status) {
            case ACTIVE -> "利用中";
            case SUSPENDED -> "休止";
            case CLOSED -> "終了";
        };
    }

    private static boolean matches(BeneficiaryDtos.BeneficiaryResponse b, String needle) {
        return contains(b.anonymizedCode(), needle)
                || contains(b.recipientNumber(), needle)
                || contains(b.municipalityCode(), needle)
                || contains(b.municipalityName(), needle)
                || contains(b.category() != null ? b.category().name() : null, needle)
                || contains(b.status() != null ? b.status().name() : null, needle)
                || contains(b.staffName(), needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimmed = blankToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
