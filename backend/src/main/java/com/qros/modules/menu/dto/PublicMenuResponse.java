package com.qros.modules.menu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.settings.model.SystemSettings;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public final class PublicMenuResponse {
        private PublicMenuResponse() {
        }

        public record Table(Long id, String tableNumber) {
        }

        public record CategoryItem(Integer id, String name, String img) {
        }

        public record Settings(
                        String restaurantName,
                        String restaurantAddress,
                        String restaurantPhone,
                        String restaurantLogoUrl,
                        Boolean enableAiAssistant) {
        }

        public record MenuItemItem(
                        Long id,
                        String name,
                        String img,
                        BigDecimal price,
                        CategorySummary category,
                        List<ItemOption> itemOptions) {
        }

        public record CategorySummary(Integer id, String name) {
        }

        public record ItemOption(
                        Long id,
                        String name,
                        @JsonProperty("isRequired") boolean isRequired,
                        int maxSelection,
                        List<ItemOptionValue> optionValues) {
        }

        public record ItemOptionValue(Long id, String name, BigDecimal extraPrice) {
        }

        public record ComboItem(
                        Long id,
                        String name,
                        BigDecimal price,
                        List<ComboLine> items) {
        }

        public record ComboLine(String menuItemName, String menuItemImg, Integer quantity) {
        }

        public record Order(
                        Long id,
                        String status,
                        BigDecimal finalAmount,
                        Table table,
                        List<OrderItem> orderItems,
                        LocalDateTime createdAt) {
        }

        public record OrderItem(
                        Long id,
                        MenuItemSummary menuItem,
                        ComboSummary combo,
                        BigDecimal unitPrice,
                        int quantity,
                        String notes,
                        String status,
                        List<OrderItemOption> options) {
        }

        public record MenuItemSummary(Long id, String name, CategoryName category) {
        }

        public record CategoryName(String name) {
        }

        public record ComboSummary(Long id, String name, BigDecimal price) {
        }

        public record OrderItemOption(String optionName, String optionValueName, BigDecimal extraPrice) {
        }

        public static CategoryItem fromCategory(Category category) {
                return new CategoryItem(category.getId(), category.getName(), category.getImg());
        }

        public static Settings fromSettings(SystemSettings settings) {
                return new Settings(
                                settings.getRestaurantName(),
                                settings.getRestaurantAddress(),
                                settings.getRestaurantPhone(),
                                settings.getRestaurantLogoUrl(),
                                settings.getEnableAiAssistant());
        }

        public static MenuItemItem fromMenuItem(MenuItem item) {
                List<ItemOption> options = item.getItemOptions() == null
                                ? Collections.emptyList()
                                : item.getItemOptions().stream()
                                                .filter(option -> !option.isDeleted())
                                                .map(option -> new ItemOption(
                                                                option.getId(),
                                                                option.getName(),
                                                                option.isRequired(),
                                                                option.getMaxSelection(),
                                                                option.getOptionValues() == null
                                                                                ? Collections.emptyList()
                                                                                : option.getOptionValues().stream()
                                                                                                .filter(value -> !value
                                                                                                                .isDeleted())
                                                                                                .map(value -> new ItemOptionValue(
                                                                                                                value.getId(),
                                                                                                                value.getName(),
                                                                                                                value.getExtraPrice()))
                                                                                                .toList()))
                                                .toList();

                return new MenuItemItem(
                                item.getId(),
                                item.getName(),
                                item.getImg(),
                                item.getPrice(),
                                item.getCategory() != null
                                                ? new CategorySummary(item.getCategory().getId(),
                                                                item.getCategory().getName())
                                                : null,
                                options);
        }

        public static ComboItem fromCombo(Combo combo) {
                List<ComboLine> lines = combo.getItems() == null
                                ? Collections.emptyList()
                                : combo.getItems().stream()
                                                .map(item -> new ComboLine(
                                                                item.getMenuItem().getName(),
                                                                item.getMenuItem().getImg(),
                                                                item.getQuantity()))
                                                .toList();

                return new ComboItem(combo.getId(), combo.getName(), combo.getPrice(), lines);
        }

        public static MenuItemItem fromMenuItemResponse(MenuItemResponse item) {
                List<ItemOption> options = item.getItemOptions() == null
                                ? Collections.emptyList()
                                : item.getItemOptions().stream()
                                                .map(option -> new ItemOption(
                                                                option.getId(),
                                                                option.getName(),
                                                                option.isRequired(),
                                                                option.getMaxSelection(),
                                                                option.getOptionValues() == null
                                                                                ? Collections.emptyList()
                                                                                : option.getOptionValues().stream()
                                                                                                .map(value -> new ItemOptionValue(
                                                                                                                value.getId(),
                                                                                                                value.getName(),
                                                                                                                value.getExtraPrice()))
                                                                                                .toList()))
                                                .toList();

                return new MenuItemItem(
                                item.getId(),
                                item.getName(),
                                item.getImg(),
                                item.getPrice(),
                                item.getCategory() != null
                                                ? new CategorySummary(item.getCategory().getId(),
                                                                item.getCategory().getName())
                                                : null,
                                options);
        }
}
