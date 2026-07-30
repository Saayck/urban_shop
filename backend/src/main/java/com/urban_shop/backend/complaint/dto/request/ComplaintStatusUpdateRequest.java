package com.urban_shop.backend.complaint.dto.request;

import com.urban_shop.backend.complaint.entity.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ComplaintStatusUpdateRequest(

    @NotNull ComplaintStatus status,

    /**
     * Respuesta al consumidor. Obligatoria al pasar a ANSWERED: el Libro de
     * Reclamaciones exige dejar constancia escrita de la respuesta.
     */
    @Size(max = 4000) String response
) {
}
