package com.urban_shop.backend.tenant;

import com.urban_shop.backend.tenant.dto.request.UpdateTenantSettingsRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsNonHttpVisualUrls() {
        UpdateTenantSettingsRequest request = new UpdateTenantSettingsRequest(
            null,
            "javascript:alert(1)",
            "ftp://example.com/banner.png",
            "#000000",
            "#FFFFFF",
            "#FF00AA",
            "Inter",
            "987654321",
            "https://instagram.com/urban",
            null,
            null
        );

        assertThat(validator.validate(request))
            .extracting(violation -> violation.getPropertyPath().toString())
            .containsExactlyInAnyOrder("logoUrl", "bannerUrl");
    }
}
