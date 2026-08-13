package com.disabilityclaim.web.dto;

import com.disabilityclaim.domain.enums.BeneficiaryCategory;
import com.disabilityclaim.domain.enums.BeneficiaryStatus;
import com.disabilityclaim.domain.enums.ServiceCategory;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public final class BeneficiaryDtos {

    private BeneficiaryDtos() {
    }

    /**
     * UI / API 両対応。匿名コード運用時は anonymizedCode + municipalityCode を優先する。
     */
    public record BeneficiaryRequest(
            UUID officeId,
            @NotNull BeneficiaryCategory category,
            String anonymizedCode,
            String recipientNumber,
            String familyName,
            String givenName,
            String familyNameKana,
            String givenNameKana,
            LocalDate birthDate,
            BeneficiaryStatus status,
            /** フロント互換: INACTIVE は CLOSED に正規化する */
            String statusCode,
            LocalDate serviceStartDate,
            LocalDate serviceEndDate,
            UUID municipalityId,
            String municipalityCode,
            UUID primaryStaffId,
            String notes
    ) {
    }

    public record BeneficiaryResponse(
            UUID id,
            UUID officeId,
            String anonymizedCode,
            BeneficiaryCategory category,
            String recipientNumber,
            String familyName,
            String givenName,
            BeneficiaryStatus status,
            /** 画面表示用。CLOSED は INACTIVE としても読めるよう併記しない（status を正とする） */
            String statusLabel,
            UUID municipalityId,
            String municipalityCode,
            String municipalityName,
            UUID primaryStaffId,
            String staffName,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    public record CertificateRequest(
            @jakarta.validation.constraints.NotBlank String certificateNumber,
            UUID municipalityId,
            String municipalityCode,
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
            String municipalityCode,
            String municipalityName,
            LocalDate validFrom,
            LocalDate validTo,
            ServiceCategory serviceCategory,
            Integer monitoringMonths,
            Integer monitoringPeriodMonths
    ) {
    }
}
