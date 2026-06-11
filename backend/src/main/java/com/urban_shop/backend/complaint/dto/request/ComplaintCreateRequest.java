package com.urban_shop.backend.complaint.dto.request;

import com.urban_shop.backend.complaint.entity.ComplaintType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ComplaintCreateRequest(
    @NotBlank @Size(max = 150) String fullName,
    @Pattern(regexp = "^(DNI|CE|PASSPORT|RUC)$", message = "documentType no valido")
    String documentType,
    @Size(max = 20) String documentNumber,
    @Email @Size(max = 150) String email,
    @NotBlank @Size(max = 20) String phone,
    @NotNull ComplaintType complaintType,
    @NotBlank @Size(max = 4000) String description,
    @Size(max = 2000) String requestedSolution
) {
}
