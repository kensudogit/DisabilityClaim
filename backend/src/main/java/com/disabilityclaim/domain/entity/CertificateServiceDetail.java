package com.disabilityclaim.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "certificate_service_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateServiceDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certificate_id", nullable = false)
    private RecipientCertificate certificate;

    @Column(name = "service_code_placeholder", length = 50)
    private String serviceCodePlaceholder;

    @Column(name = "service_name", length = 200)
    private String serviceName;

    @Column(name = "decided_units")
    private Integer decidedUnits;

    @Column(name = "decided_from")
    private LocalDate decidedFrom;

    @Column(name = "decided_to")
    private LocalDate decidedTo;

    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
