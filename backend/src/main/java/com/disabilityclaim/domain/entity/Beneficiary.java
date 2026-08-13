package com.disabilityclaim.domain.entity;

import com.disabilityclaim.domain.enums.BeneficiaryCategory;
import com.disabilityclaim.domain.enums.BeneficiaryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "beneficiaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beneficiary {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "office_id", nullable = false)
    private OfficeProfile office;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BeneficiaryCategory category;

    @Column(name = "recipient_number", length = 20)
    private String recipientNumber;

    @Column(name = "family_name", nullable = false, length = 100)
    private String familyName;

    @Column(name = "given_name", nullable = false, length = 100)
    private String givenName;

    @Column(name = "family_name_kana", length = 100)
    private String familyNameKana;

    @Column(name = "given_name_kana", length = 100)
    private String givenNameKana;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BeneficiaryStatus status;

    @Column(name = "service_start_date")
    private LocalDate serviceStartDate;

    @Column(name = "service_end_date")
    private LocalDate serviceEndDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "municipality_id")
    private Municipality municipality;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_staff_id")
    private Staff primaryStaff;

    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = BeneficiaryStatus.ACTIVE;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
