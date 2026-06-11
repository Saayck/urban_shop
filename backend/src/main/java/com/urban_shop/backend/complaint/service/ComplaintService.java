package com.urban_shop.backend.complaint.service;

import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.complaint.dto.request.ComplaintCreateRequest;
import com.urban_shop.backend.complaint.dto.request.ComplaintStatusUpdateRequest;
import com.urban_shop.backend.complaint.dto.response.ComplaintResponse;
import com.urban_shop.backend.complaint.entity.ComplaintStatus;

import java.util.UUID;

public interface ComplaintService {

    ComplaintResponse create(String tenantSlug, UUID customerId, ComplaintCreateRequest request);

    PageResponse<ComplaintResponse> listAdmin(UUID tenantId, ComplaintStatus status, int page, int size);

    ComplaintResponse getAdmin(UUID tenantId, UUID id);

    ComplaintResponse updateStatus(UUID tenantId, UUID id, ComplaintStatusUpdateRequest request);
}
