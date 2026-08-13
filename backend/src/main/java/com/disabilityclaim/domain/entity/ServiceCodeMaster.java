package com.disabilityclaim.domain.entity;

import com.disabilityclaim.domain.enums.ServiceCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "service_code_masters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCodeMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_code", nullable = false, length = 50)
    private String serviceCode;

    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_category", length = 40)
    private ServiceCategory serviceCategory;

    private Integer units;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "source_document", nullable = false, length = 500)
    private String sourceDocument;

    @Column(name = "source_version", nullable = false, length = 100)
    private String sourceVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
