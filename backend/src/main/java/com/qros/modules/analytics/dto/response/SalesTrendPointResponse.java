package com.qros.modules.analytics.dto.response;

import java.time.LocalDate;

public record SalesTrendPointResponse(LocalDate date, Long quantitySold) {}
