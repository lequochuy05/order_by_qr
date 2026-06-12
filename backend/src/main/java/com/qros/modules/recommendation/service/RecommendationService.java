package com.qros.modules.recommendation.service;

import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.modules.order.repository.OrderItemRepository;
import com.qros.modules.recommendation.dto.response.RecommendationResponse;
import com.qros.modules.recommendation.mapper.RecommendationMapper;
import com.qros.modules.recommendation.model.enums.RecommendationContext;
import com.qros.modules.recommendation.model.enums.RecommendationType;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    private final MenuItemRepository menuItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final RecommendationMapper recommendationMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.RECOMMENDATIONS, key = "'personalized:' + (#context != null ? #context.name() : 'ANY') + ':' + #limit")
    public RecommendationResponse getPersonalizedRecommendations(
            RecommendationContext context,
            int limit) {
        int safeLimit = sanitizeLimit(limit);
        RecommendationContext safeContext = context != null ? context : RecommendationContext.ANY;

        List<MenuItem> candidates = findByContext(safeContext, safeLimit);
        List<MenuItem> items = fillWithPopularItems(candidates, safeLimit);

        return recommendationMapper.toResponse(
                RecommendationType.PERSONALIZED,
                safeContext,
                safeLimit,
                items,
                reasonForContext(safeContext));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.RECOMMENDATIONS, key = "'popular:' + #limit")
    public RecommendationResponse getPopularItems(int limit) {
        int safeLimit = sanitizeLimit(limit);

        List<MenuItem> items = orderItemRepository.findPopularAvailableMenuItems(
                PageRequest.of(0, safeLimit));

        if (items.isEmpty()) {
            items = findLatestAvailableItems(safeLimit);
        }

        return recommendationMapper.toResponse(
                RecommendationType.POPULAR,
                RecommendationContext.ANY,
                safeLimit,
                items,
                "Món đang được gọi nhiều");
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.RECOMMENDATIONS, key = "'crossSell:' + #itemId + ':' + #limit")
    public RecommendationResponse getCrossSellRecommendations(
            @NonNull Long itemId,
            int limit) {
        int safeLimit = sanitizeLimit(limit);
        MenuItem sourceItem = getPublicAvailableMenuItem(itemId);

        List<MenuItem> candidates = orderItemRepository.findCrossSellAvailableMenuItems(
                itemId,
                PageRequest.of(0, safeLimit));

        List<MenuItem> items = fillWithSimilarAndPopularItems(
                candidates,
                sourceItem,
                safeLimit);

        return recommendationMapper.toResponse(
                RecommendationType.CROSS_SELL,
                RecommendationContext.ANY,
                safeLimit,
                items,
                "Thường được gọi cùng món này");
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.RECOMMENDATIONS, key = "'similar:' + #itemId + ':' + #limit")
    public RecommendationResponse getRecommendations(
            @NonNull Long itemId,
            int limit) {
        int safeLimit = sanitizeLimit(limit);
        MenuItem sourceItem = getPublicAvailableMenuItem(itemId);

        List<MenuItem> candidates = findSimilarItems(sourceItem, safeLimit);
        List<MenuItem> items = fillWithPopularItemsExcluding(
                candidates,
                sourceItem.getId(),
                safeLimit);

        return recommendationMapper.toResponse(
                RecommendationType.SIMILAR,
                RecommendationContext.ANY,
                safeLimit,
                items,
                "Cùng danh mục với món bạn đang xem");
    }

    private MenuItem getPublicAvailableMenuItem(Long itemId) {
        return menuItemRepository.findById(itemId)
                .filter(this::isPublicAvailable)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_ITEM_NOT_FOUND));
    }

    private List<MenuItem> findByContext(RecommendationContext context, int limit) {
        if (context == RecommendationContext.ANY) {
            return orderItemRepository.findPopularAvailableMenuItems(PageRequest.of(0, limit));
        }

        List<MenuItem> activeItems = menuItemRepository.findAllByActiveTrueOrderByDisplayOrderAscNameAsc();

        return activeItems.stream()
                .filter(this::isPublicAvailable)
                .filter(item -> matchesContext(item, context))
                .limit(limit)
                .toList();
    }

    private List<MenuItem> findSimilarItems(MenuItem sourceItem, int limit) {
        Category category = sourceItem.getCategory();

        if (category == null || category.getId() == null) {
            return List.of();
        }

        return menuItemRepository.findSimilarAvailableItems(
                category.getId(),
                sourceItem.getId(),
                PageRequest.of(0, limit));
    }

    private List<MenuItem> fillWithSimilarAndPopularItems(
            List<MenuItem> candidates,
            MenuItem sourceItem,
            int limit) {
        List<MenuItem> merged = new ArrayList<>(candidates);

        if (merged.size() < limit) {
            merged.addAll(findSimilarItems(sourceItem, limit));
        }

        if (merged.size() < limit) {
            merged.addAll(orderItemRepository.findPopularAvailableMenuItems(PageRequest.of(0, limit)));
        }

        if (merged.size() < limit) {
            merged.addAll(findLatestAvailableItems(limit));
        }

        return distinctLimitExcluding(merged, sourceItem.getId(), limit);
    }

    private List<MenuItem> fillWithPopularItems(List<MenuItem> candidates, int limit) {
        return fillWithPopularItemsExcluding(candidates, null, limit);
    }

    private List<MenuItem> fillWithPopularItemsExcluding(
            List<MenuItem> candidates,
            Long excludedItemId,
            int limit) {
        List<MenuItem> merged = new ArrayList<>(candidates);

        if (merged.size() < limit) {
            merged.addAll(orderItemRepository.findPopularAvailableMenuItems(PageRequest.of(0, limit)));
        }

        if (merged.size() < limit) {
            merged.addAll(findLatestAvailableItems(limit));
        }

        return distinctLimitExcluding(merged, excludedItemId, limit);
    }

    private List<MenuItem> findLatestAvailableItems(int limit) {
        return menuItemRepository.findByActiveTrueAndAvailableTrueOrderByIdDesc(
                PageRequest.of(0, limit));
    }

    private List<MenuItem> distinctLimitExcluding(
            List<MenuItem> items,
            Long excludedItemId,
            int limit) {
        Map<Long, MenuItem> distinctItems = new LinkedHashMap<>();

        for (MenuItem item : items) {
            if (item == null || item.getId() == null) {
                continue;
            }

            if (excludedItemId != null && excludedItemId.equals(item.getId())) {
                continue;
            }

            if (!isPublicAvailable(item)) {
                continue;
            }

            distinctItems.putIfAbsent(item.getId(), item);

            if (distinctItems.size() >= limit) {
                break;
            }
        }

        return new ArrayList<>(distinctItems.values());
    }

    private boolean isPublicAvailable(MenuItem item) {
        if (item == null) {
            return false;
        }

        Category category = item.getCategory();

        return Boolean.TRUE.equals(item.getActive())
                && Boolean.TRUE.equals(item.getAvailable())
                && category != null
                && Boolean.TRUE.equals(category.getActive());
    }

    private boolean matchesContext(MenuItem item, RecommendationContext context) {
        Category category = item.getCategory();

        String itemName = normalize(item.getName());
        String categoryName = category != null ? normalize(category.getName()) : "";

        return switch (context) {
            case MORNING -> containsAny(
                    itemName,
                    categoryName,
                    "tra sua",
                    "an vat",
                    "soda",
                    "nuoc",
                    "my cay");

            case LUNCH -> containsAny(
                    itemName,
                    categoryName,
                    "tra sua",
                    "an vat",
                    "soda",
                    "nuoc",
                    "my cay");

            case AFTERNOON -> containsAny(
                    itemName,
                    categoryName,
                    "tra sua",
                    "an vat",
                    "soda",
                    "nuoc",
                    "my cay");

            case DINNER -> containsAny(
                    itemName,
                    categoryName,
                    "tra sua",
                    "an vat",
                    "soda",
                    "nuoc",
                    "my cay",
                    "giai khat");

            case ANY -> true;
        };
    }

    private boolean containsAny(String itemName, String categoryName, String... keywords) {
        for (String keyword : keywords) {
            if (itemName.contains(keyword) || categoryName.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private int sanitizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, MAX_LIMIT);
    }

    private String reasonForContext(RecommendationContext context) {
        return switch (context) {
            case MORNING -> "Phù hợp buổi sáng";
            case LUNCH -> "Phù hợp bữa trưa";
            case AFTERNOON -> "Phù hợp buổi chiều";
            case DINNER -> "Phù hợp bữa tối";
            case ANY -> "Gợi ý phù hợp cho bạn";
        };
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized.toLowerCase(Locale.ROOT);
    }
}
