package com.qros.modules.menu.dto.response;
public record ComboItemResponse (
    Long id,
    Long menuItemId,
    String menuItemName,
    String menuItemImg,
    Integer quantity
) {}