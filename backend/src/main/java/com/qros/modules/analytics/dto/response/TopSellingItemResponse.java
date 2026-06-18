package com.qros.modules.analytics.dto.response;

import java.math.BigDecimal;

public record TopSellingItemResponse(
        Long menuItemId,
        String itemName,
        String categoryName,
        String imageUrl,
        Long quantitySold,
        BigDecimal revenue) {}
