package com.qros.modules.menu.dto.response;

import java.util.List;

public record ItemOptionResponse(
        Long id,
        String name,
        Boolean required,
        Integer maxSelection,
        Integer displayOrder,
        List<ItemOptionValueResponse> optionValues) {}
