package com.urban_shop.backend.media.dto;

public record MediaResponse(
    String filename,
    String url,
    String contentType,
    long size
) {
}
