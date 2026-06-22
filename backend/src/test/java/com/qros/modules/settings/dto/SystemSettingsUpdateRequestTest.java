package com.qros.modules.settings.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.qros.modules.settings.dto.request.SystemSettingsUpdateRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class SystemSettingsUpdateRequestTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsOvernightHoursAndIsoCurrency() {
        SystemSettingsUpdateRequest request =
                request("VND", "https://example.com/logo.png", "0901 234 567", LocalTime.of(18, 0), LocalTime.of(2, 0));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsUnsafeLogoUrlAndInvalidCurrency() {
        SystemSettingsUpdateRequest request =
                request("VNĐ!", "javascript:alert(1)", "0901234567", LocalTime.of(8, 0), LocalTime.of(22, 0));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("currency", "logoUrl");
    }

    @Test
    void rejectsEqualOpeningAndClosingTimes() {
        SystemSettingsUpdateRequest request = request("USD", null, null, LocalTime.of(8, 0), LocalTime.of(8, 0));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("validTimeRange");
    }

    private SystemSettingsUpdateRequest request(
            String currency, String logoUrl, String phone, LocalTime openingTime, LocalTime closingTime) {
        return new SystemSettingsUpdateRequest(
                "QROS Restaurant",
                phone,
                "contact@example.com",
                "123 Main Street",
                logoUrl,
                null,
                null,
                openingTime,
                closingTime,
                currency,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true,
                false,
                0L);
    }
}
