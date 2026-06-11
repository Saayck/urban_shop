package com.urban_shop.backend.complaint.controller;

import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.common.tenant.CurrentTenant;
import com.urban_shop.backend.complaint.dto.request.ComplaintStatusUpdateRequest;
import com.urban_shop.backend.complaint.dto.response.ComplaintResponse;
import com.urban_shop.backend.complaint.entity.ComplaintStatus;
import com.urban_shop.backend.complaint.service.ComplaintService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/complaints")
@RequiredArgsConstructor
@Validated
public class AdminComplaintController {

    private final ComplaintService complaintService;
    private final CurrentTenant currentTenant;

    @GetMapping
    public PageResponse<ComplaintResponse> list(
        @RequestParam(required = false) ComplaintStatus status,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return complaintService.listAdmin(currentTenant.requireId(), status, page, size);
    }

    @GetMapping("/{id}")
    public ComplaintResponse get(@PathVariable UUID id) {
        return complaintService.getAdmin(currentTenant.requireId(), id);
    }

    @PutMapping("/{id}/status")
    public ComplaintResponse updateStatus(
        @PathVariable UUID id,
        @Valid @RequestBody ComplaintStatusUpdateRequest request
    ) {
        return complaintService.updateStatus(currentTenant.requireId(), id, request);
    }
}
