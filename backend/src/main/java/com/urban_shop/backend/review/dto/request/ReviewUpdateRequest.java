package com.urban_shop.backend.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewUpdateRequest(

    @NotNull @Min(1) @Max(5) Short rating,

    @Size(max = 150) String title,

    @Size(max = 2000) String comment
) {
}
