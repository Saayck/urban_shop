package com.urban_shop.backend.complaint.entity;

import com.urban_shop.backend.common.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "complaints_book")
@Getter
@Setter
@NoArgsConstructor
public class ComplaintBook extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "complaint_number", nullable = false, length = 50)
    private String complaintNumber;

    @Column(name = "customer_full_name", nullable = false, length = 150)
    private String customerFullName;

    @Column(name = "customer_document_type", length = 30)
    private String customerDocumentType;

    @Column(name = "customer_document_number", length = 20)
    private String customerDocumentNumber;

    @Column(name = "customer_email", length = 150)
    private String customerEmail;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "complaint_type", nullable = false, length = 30)
    private ComplaintType complaintType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "requested_solution", columnDefinition = "TEXT")
    private String requestedSolution;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ComplaintStatus status = ComplaintStatus.OPEN;

    /** Respuesta escrita al consumidor, obligatoria para cerrar el reclamo como ANSWERED. */
    @Column(name = "response_text", columnDefinition = "TEXT")
    private String responseText;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
}
