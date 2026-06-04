package com.bank.customer.domain;

import com.bank.common.security.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Customer aggregate root. KYC status drives downstream account eligibility.
 */
@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customer_email", columnList = "email", unique = true),
        @Index(name = "idx_customer_national_id", columnList = "national_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Customer {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 500)
    private String phone;

    /**
     * National IDs are encrypted at rest. Lookups by national ID would require a
     * deterministic hash column (omitted here for brevity) — recommended next step.
     */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "national_id", nullable = false, length = 500)
    private String nationalId;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus kycStatus;

    @Embedded
    private Address address;

    @Version
    private Long version;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
