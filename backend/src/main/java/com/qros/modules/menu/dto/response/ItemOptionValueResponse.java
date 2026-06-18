package com.qros.modules.menu.dto.response;

import java.math.BigDecimal;

public record ItemOptionValueResponse(Long id, String name, BigDecimal extraPrice, Integer displayOrder) {}
