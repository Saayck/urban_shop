package com.urban_shop.backend.review.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewModerationRequest(

    @NotNull Boolean visible,

    @Size(max = 500) String moderationNote
) {
}
