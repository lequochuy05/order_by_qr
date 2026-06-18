package com.qros.modules.menu.dto.publicmenu;

import java.util.List;

public record PublicItemOption(
        Long id,
        String name,
        Boolean required,
        Integer maxSelection,
        Integer displayOrder,
        List<PublicItemOptionValue> optionValues) {}
