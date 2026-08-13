package com.disabilityclaim.domain.entity;

import com.disabilityclaim.domain.enums.ActivityType;
import com.disabilityclaim.domain.enums.ServiceCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "support_activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiary_id", nullable = false)
    private Beneficiary beneficiary;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "billing_month", nullable = false, length = 7)
    private String billingMonth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_category", length = 40)
    private ServiceCategory serviceCategory;

    private String memo;

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
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
