package com.urban_shop.backend.complaint.dto.response;

import com.urban_shop.backend.complaint.entity.ComplaintStatus;
import com.urban_shop.backend.complaint.entity.ComplaintType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ComplaintResponse(
    UUID id,
    String complaintNumber,
    UUID customerId,
    String customerFullName,
    String customerDocumentType,
    String customerDocumentNumber,
    String customerEmail,
    String customerPhone,
    ComplaintType complaintType,
    String description,
    String requestedSolution,
    ComplaintStatus status,
    String response,
    LocalDateTime respondedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
