package com.qros.modules.menu.dto.publicmenu;

public record PublicSettings(
    String restaurantName,
    String restaurantAddress,
    String restaurantPhone,
    String restaurantLogoUrl,
    Boolean enableAiAssistant
) {
}