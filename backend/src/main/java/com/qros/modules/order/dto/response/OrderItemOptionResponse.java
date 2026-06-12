package com.qros.modules.order.dto.response;

import java.math.BigDecimal;

public record OrderItemOptionResponse(
        Long valueId,
        String optionName,
        String optionValueName,
        BigDecimal extraPrice) {
}