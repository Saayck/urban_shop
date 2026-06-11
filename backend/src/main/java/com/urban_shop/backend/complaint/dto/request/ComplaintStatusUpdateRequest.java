package com.urban_shop.backend.complaint.dto.request;

import com.urban_shop.backend.complaint.entity.ComplaintStatus;
import jakarta.validation.constraints.NotNull;

public record ComplaintStatusUpdateRequest(
    @NotNull ComplaintStatus status
) {
}
