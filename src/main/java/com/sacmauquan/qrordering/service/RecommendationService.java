package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.repository.MenuItemRepository;
import com.sacmauquan.qrordering.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;

    // ========================
    // PUBLIC API
    // ========================

    /**
     * Gợi ý món thường được đặt cùng với món đã cho.
     * Fallback: trả về top bán chạy nếu không có dữ liệu association.
     */
    public List<MenuItem> getRecommendations(Long itemId, int limit) {
        if (itemId == null) return List.of();

        List<Long> associatedIds = orderItemRepository.findTopAssociatedItems(itemId, limit);
        if (associatedIds == null || associatedIds.isEmpty()) {
            return getPopularItems(limit);
        }
        return preserveOrder(associatedIds);
    }

    /**
     * Gợi ý cá nhân hóa dựa trên context (thời gian, thời tiết).
     * Sử dụng scoring system: Time (40%) + Weather (30%) + Popularity (30%).
     */
    public List<MenuItem> getPersonalizedRecommendations(String timeContext, String weatherContext, int limit) {
        List<MenuItem> allMenu = menuItemRepository.findAll();
        String timeKey = normalize(timeContext);
        String weatherKey = normalize(weatherContext);

        return allMenu.stream()
                .filter(item -> item.getCategory() != null)
                .map(item -> Map.entry(item, calculateRelevanceScore(item, timeKey, weatherKey)))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<MenuItem, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Cross-sell: gợi ý món bổ sung khi thêm vào giỏ hàng.
     * Ưu tiên: frequency-based → similarity scoring → popular fallback.
     */
    public List<MenuItem> getCrossSellRecommendations(Long currentItemId, int limit) {
        MenuItem currentItem = menuItemRepository.findById(currentItemId).orElse(null);
        if (currentItem == null) return getPopularItems(limit);

        // 1. Ưu tiên dữ liệu frequency thực tế
        List<Long> associatedIds = orderItemRepository.findTopAssociatedItems(currentItemId, limit);
        if (associatedIds != null && !associatedIds.isEmpty()) {
            return preserveOrder(associatedIds);
        }

        // 2. Fallback: similarity scoring
        if (currentItem.getCategory() == null) return getPopularItems(limit);
        List<MenuItem> allMenu = menuItemRepository.findAll();

        return allMenu.stream()
                .filter(item -> !item.getId().equals(currentItemId))
                .filter(item -> item.getCategory() != null)
                .map(item -> Map.entry(item, calculateSimilarity(currentItem, item)))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<MenuItem, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Top bán chạy thực sự, dựa trên đơn hàng đã thanh toán.
     * Fallback: món mới nhất nếu chưa có dữ liệu bán hàng.
     */
    public List<MenuItem> getPopularItems(int limit) {
        List<Long> topIds = orderItemRepository.findTopSellingItemIds(limit);

        if (topIds == null || topIds.isEmpty()) {
            return menuItemRepository.findAll().stream()
                    .sorted(Comparator.comparing(MenuItem::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(limit)
                    .collect(Collectors.toList());
        }
        return preserveOrder(topIds);
    }

    // ========================
    // SCORING ENGINE
    // ========================

    private double calculateRelevanceScore(MenuItem item, String timeKey, String weatherKey) {
        String cateName = item.getCategory().getName().toLowerCase();

        double timeScore = calculateTimeScore(cateName, timeKey) * 40;
        double weatherScore = calculateWeatherScore(cateName, weatherKey) * 30;
        double popularityScore = calculatePopularityScore(item.getId()) * 30;

        return timeScore + weatherScore + popularityScore;
    }

    private double calculateSimilarity(MenuItem current, MenuItem candidate) {
        double score = 0;
        String currentCate = current.getCategory().getName().toLowerCase();
        String candidateCate = candidate.getCategory().getName().toLowerCase();

        // Complementary category bonus (food↔drink)
        if (isComplementary(currentCate, candidateCate)) {
            score += 50;
        }

        // Price range similarity (±30%)
        double priceRatio = candidate.getPrice() / Math.max(current.getPrice(), 1);
        if (priceRatio >= 0.7 && priceRatio <= 1.3) {
            score += 30;
        }

        // Popularity boost
        score += calculatePopularityScore(candidate.getId()) * 20;

        return score;
    }

    // ========================
    // SCORING HELPERS
    // ========================

    private double calculateTimeScore(String cateName, String timeKey) {
        if (timeKey.isEmpty()) return 0;

        if (containsAny(timeKey, "sáng", "morning")) {
            return containsAny(cateName, "sáng", "phở", "bún", "cà phê", "bánh mì") ? 1.0 : 0;
        }
        if (containsAny(timeKey, "trưa", "noon", "lunch")) {
            return containsAny(cateName, "cơm", "mỳ", "văn phòng", "set lunch") ? 1.0 : 0;
        }
        if (containsAny(timeKey, "tối", "evening", "night")) {
            return containsAny(cateName, "lẩu", "nhậu", "bia", "nướng") ? 1.0 : 0;
        }
        return 0;
    }

    private double calculateWeatherScore(String cateName, String weatherKey) {
        if (weatherKey.isEmpty()) return 0;

        if (containsAny(weatherKey, "mưa", "rain", "lạnh", "cold")) {
            return containsAny(cateName, "nóng", "lẩu", "súp", "cháo") ? 1.0 : 0;
        }
        if (containsAny(weatherKey, "nắng", "sun", "nóng", "hot")) {
            return containsAny(cateName, "lạnh", "giải khát", "kem", "sinh tố", "trà") ? 1.0 : 0;
        }
        return 0;
    }

    private double calculatePopularityScore(Long itemId) {
        long totalSold = orderItemRepository.countTotalSoldByItemId(itemId);
        return Math.min(totalSold / 100.0, 1.0);
    }

    // ========================
    // CATEGORY CLASSIFICATION
    // ========================

    private boolean isComplementary(String catA, String catB) {
        return (isFood(catA) && isDrinkOrTopping(catB)) || (isDrinkOrTopping(catA) && isFood(catB));
    }

    private boolean isFood(String category) {
        return containsAny(category, "cơm", "phở", "bún", "món", "ăn", "bánh", "mỳ", "lẩu", "nướng");
    }

    private boolean isDrinkOrTopping(String category) {
        return containsAny(category, "uống", "nước", "trà", "topping", "thêm", "kem", "sinh tố", "cà phê");
    }

    // ========================
    // UTILITIES
    // ========================

    private String normalize(String input) {
        return input != null ? input.toLowerCase().trim() : "";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    /**
     * Truy vấn MenuItem theo danh sách ID, giữ nguyên thứ tự ranking từ DB.
     */
    private List<MenuItem> preserveOrder(List<Long> ids) {
        List<MenuItem> items = menuItemRepository.findAllById(ids);
        Map<Long, MenuItem> itemMap = items.stream()
                .collect(Collectors.toMap(MenuItem::getId, Function.identity()));
        return ids.stream()
                .map(itemMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
