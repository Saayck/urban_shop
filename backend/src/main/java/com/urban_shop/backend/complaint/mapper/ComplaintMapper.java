package com.urban_shop.backend.complaint.mapper;

import com.urban_shop.backend.complaint.dto.response.ComplaintResponse;
import com.urban_shop.backend.complaint.entity.ComplaintBook;

public final class ComplaintMapper {

    private ComplaintMapper() {
    }

    public static ComplaintResponse toResponse(ComplaintBook complaint) {
        return new ComplaintResponse(
            complaint.getId(),
            complaint.getComplaintNumber(),
            complaint.getCustomerId(),
            complaint.getCustomerFullName(),
            complaint.getCustomerDocumentType(),
            complaint.getCustomerDocumentNumber(),
            complaint.getCustomerEmail(),
            complaint.getCustomerPhone(),
            complaint.getComplaintType(),
            complaint.getDescription(),
            complaint.getRequestedSolution(),
            complaint.getStatus(),
            complaint.getResponseText(),
            complaint.getRespondedAt(),
            complaint.getCreatedAt(),
            complaint.getUpdatedAt()
        );
    }
}
