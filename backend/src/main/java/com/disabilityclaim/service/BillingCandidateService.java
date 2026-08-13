package com.disabilityclaim.service;

import com.disabilityclaim.domain.entity.Beneficiary;
import com.disabilityclaim.domain.entity.RecipientCertificate;
import com.disabilityclaim.domain.entity.SupportActivity;
import com.disabilityclaim.repository.RecipientCertificateRepository;
import com.disabilityclaim.repository.SupportActivityRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingCandidateService {

    private final RecipientCertificateRepository certificateRepository;
    private final SupportActivityRepository supportActivityRepository;

    @Transactional(readOnly = true)
    public List<BillingCandidate> findCandidates(String billingMonth, UUID officeId) {
        YearMonth ym = YearMonth.parse(billingMonth);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        List<RecipientCertificate> validCerts = certificateRepository.findValidInPeriod(monthStart, monthEnd);
        List<BillingCandidate> result = new ArrayList<>();
        for (RecipientCertificate cert : validCerts) {
            Beneficiary beneficiary = cert.getBeneficiary();
            if (officeId != null && !beneficiary.getOffice().getId().equals(officeId)) {
                continue;
            }
            if (beneficiary.getMunicipality() == null && cert.getMunicipality() == null) {
                continue;
            }
            boolean hasActivity = supportActivityRepository.existsByBeneficiaryIdAndBillingMonth(
                    beneficiary.getId(), billingMonth);
            if (!hasActivity) {
                continue;
            }
            List<SupportActivity> activities = supportActivityRepository
                    .findByBeneficiaryIdAndBillingMonth(beneficiary.getId(), billingMonth);
            result.add(BillingCandidate.builder()
                    .beneficiaryId(beneficiary.getId())
                    .certificateId(cert.getId())
                    .municipalityId(cert.getMunicipality() != null
                            ? cert.getMunicipality().getId()
                            : beneficiary.getMunicipality().getId())
                    .serviceCategory(cert.getServiceCategory())
                    .category(beneficiary.getCategory())
                    .activityCount(activities.size())
                    .build());
        }
        return result;
    }

    @Getter
    @Builder
    public static class BillingCandidate {
        private final UUID beneficiaryId;
        private final UUID certificateId;
        private final UUID municipalityId;
        private final com.disabilityclaim.domain.enums.ServiceCategory serviceCategory;
        private final com.disabilityclaim.domain.enums.BeneficiaryCategory category;
        private final int activityCount;
    }
}
