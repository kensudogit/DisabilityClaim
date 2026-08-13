package com.disabilityclaim.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "office_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfficeProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "office_code", nullable = false, unique = true, length = 20)
    private String officeCode;

    @Column(name = "office_name", nullable = false, length = 200)
    private String officeName;

    @Column(name = "corporation_name", length = 200)
    private String corporationName;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    private String address;

    private String phone;

    @Column(name = "region_category_code", length = 20)
    private String regionCategoryCode;

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
