package com.disabilityclaim.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "office_qualification_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfficeQualificationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "office_id", nullable = false)
    private OfficeProfile office;

    @Column(name = "qualification_code", nullable = false, length = 50)
    private String qualificationCode;

    @Column(name = "qualification_name", nullable = false, length = 200)
    private String qualificationName;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "source_document", length = 500)
    private String sourceDocument;

    @Column(name = "source_version", length = 100)
    private String sourceVersion;

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
