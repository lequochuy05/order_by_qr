package com.qros.modules.menu.dto.publicmenu;

import java.math.BigDecimal;

public record PublicItemOptionValue(Long id, String name, BigDecimal extraPrice, Integer displayOrder) {}
