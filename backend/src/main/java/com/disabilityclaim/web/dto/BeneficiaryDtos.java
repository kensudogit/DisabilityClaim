package com.disabilityclaim.web.dto;

import com.disabilityclaim.domain.enums.BeneficiaryCategory;
import com.disabilityclaim.domain.enums.BeneficiaryStatus;
import com.disabilityclaim.domain.enums.ServiceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public final class BeneficiaryDtos {

    private BeneficiaryDtos() {
    }

    public record BeneficiaryRequest(
            @NotNull UUID officeId,
            @NotNull BeneficiaryCategory category,
            String recipientNumber,
            @NotBlank String familyName,
            @NotBlank String givenName,
            String familyNameKana,
            String givenNameKana,
            LocalDate birthDate,
            BeneficiaryStatus status,
            LocalDate serviceStartDate,
            LocalDate serviceEndDate,
            UUID municipalityId,
            UUID primaryStaffId,
            String notes
    ) {
    }

    public record BeneficiaryResponse(
            UUID id,
            UUID officeId,
            BeneficiaryCategory category,
            String recipientNumber,
            String familyName,
            String givenName,
            BeneficiaryStatus status,
            UUID municipalityId,
            UUID primaryStaffId
    ) {
    }

    public record CertificateRequest(
            @NotBlank String certificateNumber,
            @NotNull UUID municipalityId,
            @NotNull LocalDate validFrom,
            @NotNull LocalDate validTo,
            @NotNull ServiceCategory serviceCategory,
            Integer monitoringMonths,
            String notes
    ) {
    }

    public record CertificateResponse(
            UUID id,
            UUID beneficiaryId,
            String certificateNumber,
            UUID municipalityId,
            LocalDate validFrom,
            LocalDate validTo,
            ServiceCategory serviceCategory,
            Integer monitoringMonths
    ) {
    }
}
