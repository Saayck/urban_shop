package com.urban_shop.backend.complaint.controller;

import com.urban_shop.backend.common.security.CustomUserDetails;
import com.urban_shop.backend.complaint.dto.request.ComplaintCreateRequest;
import com.urban_shop.backend.complaint.dto.response.ComplaintResponse;
import com.urban_shop.backend.complaint.service.ComplaintService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@SecurityRequirements
@RequestMapping("/api/store/{slug}/complaints")
@RequiredArgsConstructor
public class StoreComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintResponse create(
        @PathVariable String slug,
        @AuthenticationPrincipal CustomUserDetails principal,
        @Valid @RequestBody ComplaintCreateRequest request
    ) {
        UUID customerId = (principal != null && principal.isCustomer())
            ? principal.getPrincipalId()
            : null;
        return complaintService.create(slug, customerId, request);
    }
}
