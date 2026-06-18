package com.qros.modules.analytics.repository.projection;

import java.math.BigDecimal;

public interface TopSellingItemProjection {

    Long getMenuItemId();

    String getItemName();

    String getCategoryName();

    String getImageUrl();

    Long getQuantitySold();

    BigDecimal getRevenue();
}
