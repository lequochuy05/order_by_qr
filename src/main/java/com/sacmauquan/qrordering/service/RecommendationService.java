package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.MenuItemResponse;
import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.repository.MenuItemRepository;
import com.sacmauquan.qrordering.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RecommendationService - Hệ thống gợi ý món ăn thông minh dựa trên lịch sử đặt
 * hàng và ngữ cảnh.
 * Đã nâng cấp chuẩn Senior: Tách biệt hoàn toàn Entity và DTO, tối ưu hóa hiệu
 * năng nạp dữ liệu (Batch fetching).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;

    /**
     * Gợi ý món ăn thường được đặt cùng với một món cụ thể (Cross-sell).
     * Phân tích lịch sử đơn hàng để tìm ra các cặp món phổ biến.
     */
    @Cacheable(value = "recommendations", key = "'cross_' + #itemId + '_' + #limit")
    public List<MenuItemResponse> getCrossSellRecommendations(Long itemId, int limit) {
        if (itemId == null)
            return getPopularItems(limit);

        List<Long> associatedIds = orderItemRepository.findTopAssociatedItems(itemId, limit * 2);

        if (associatedIds.isEmpty())
            return getPopularItems(limit);

        return menuItemRepository.findAllById(associatedIds).stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .limit(limit)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách món ăn tương tự (Dùng chung logic với Cross-sell nhưng có thể
     * mở rộng sau này).
     */
    @Cacheable(value = "recommendations", key = "'similar_' + #itemId + '_' + #limit")
    public List<MenuItemResponse> getRecommendations(Long itemId, int limit) {
        return getCrossSellRecommendations(itemId, limit);
    }

    /**
     * Lấy danh sách món ăn bán chạy nhất hệ thống.
     * Có tích hợp Cache để giảm tải cho các phép tính Aggregate của Database.
     */
    @Cacheable(value = "popularItems", key = "'pop_' + #limit")
    public List<MenuItemResponse> getPopularItems(int limit) {
        List<Long> topIds = orderItemRepository.findTopSellingItemIds(limit * 2);

        List<MenuItem> items;
        if (topIds == null || topIds.isEmpty()) {
            items = menuItemRepository.findAllByActiveTrue().stream()
                    .sorted(Comparator.comparing(MenuItem::getCreatedAt).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
        } else {
            items = menuItemRepository.findAllById(topIds).stream()
                    .filter(item -> Boolean.TRUE.equals(item.getActive()))
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        return items.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Hệ thống gợi ý cá nhân hóa dựa trên ngữ cảnh thời gian và thời tiết.
     * Sử dụng thuật toán Weighted Scoring để chấm điểm món ăn phù hợp.
     */
    public List<MenuItemResponse> getPersonalizedRecommendations(String timeContext, String weatherContext, int limit) {
        List<MenuItem> activeMenu = menuItemRepository.findAllByActiveTrue();
        if (activeMenu.isEmpty())
            return List.of();

        Map<Long, Long> popularityMap = getPopularityMap(activeMenu);
        String tKey = normalize(timeContext);
        String wKey = normalize(weatherContext);

        return activeMenu.stream()
                .map(item -> {
                    double score = calculateItemScore(item, tKey, wKey, popularityMap.getOrDefault(item.getId(), 0L));
                    return new AbstractMap.SimpleEntry<>(item, score);
                })
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<MenuItem, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> convertToResponse(entry.getKey()))
                .collect(Collectors.toList());
    }

    private double calculateItemScore(MenuItem item, String tKey, String wKey, long soldCount) {
        if (item.getCategory() == null)
            return 0;

        String cateName = item.getCategory().getName().toLowerCase();

        // Trọng số gợi ý: Thời gian (40%) + Thời tiết (30%) + Độ phổ biến thực tế (30%)
        double timeScore = calculateTimeMatch(cateName, tKey) * 40;
        double weatherScore = calculateWeatherMatch(cateName, wKey) * 30;
        double popularityScore = Math.min(soldCount / 50.0, 1.0) * 30;

        return timeScore + weatherScore + popularityScore;
    }

    private double calculateTimeMatch(String cateName, String tKey) {
        if (tKey.isBlank())
            return 0.5;
        if (containsAny(tKey, "sáng", "morning") &&
                containsAny(cateName, "Mỳ cay", "Trà", "Soda", "Giải Khát", "Trà Sữa"))
            return 1.0;
        if (containsAny(tKey, "trưa", "lunch") &&
                containsAny(cateName, "Ăn Vặt", "Trà Sữa", "Nước Ép"))
            return 1.0;
        if (containsAny(tKey, "tối", "dinner") &&
                containsAny(cateName, "Ăn Vặt", "Giải Khát", "Mỳ cay"))
            return 1.0;
        return 0.1;
    }

    private double calculateWeatherMatch(String cateName, String wKey) {
        if (wKey.isBlank())
            return 0.5;
        if (containsAny(wKey, "nóng", "hot") &&
                containsAny(cateName, "Trà", "Soda", "Nước Ép", "Giải Khát"))
            return 1.0;
        if (containsAny(wKey, "lạnh", "mưa", "cold") &&
                containsAny(cateName, "Mỳ cay", "Ăn Vặt"))
            return 1.0;
        return 0.1;
    }

    private Map<Long, Long> getPopularityMap(List<MenuItem> items) {
        List<Long> ids = items.stream().map(MenuItem::getId).collect(Collectors.toList());
        List<Object[]> results = orderItemRepository.countTotalSoldBatch(ids);
        return results.stream().collect(Collectors.toMap(
                res -> (Long) res[0],
                res -> (Long) res[1]));
    }

    private MenuItemResponse convertToResponse(MenuItem item) {
        return MenuItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .price(item.getPrice())
                .img(item.getImg())
                .active(item.getActive())
                .category(item.getCategory() != null ? MenuItemResponse.CategorySummary.builder()
                        .id(item.getCategory().getId())
                        .name(item.getCategory().getName())
                        .build() : null)
                .build();
    }

    private String normalize(String input) {
        return input != null ? input.toLowerCase().trim() : "";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw.toLowerCase()))
                return true;
        }
        return false;
    }
}
