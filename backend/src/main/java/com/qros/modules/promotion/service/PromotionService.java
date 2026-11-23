package com.qros.modules.promotion.service;

import com.qros.modules.promotion.dto.request.PromotionRequest;
import com.qros.modules.promotion.dto.response.PromotionResponse;
import com.qros.modules.promotion.mapper.PromotionMapper;
import com.qros.modules.promotion.model.Promotion;
import com.qros.modules.promotion.repository.PromotionRepository;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.*;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int MONEY_SCALE = 2;

    private final PromotionRepository promotionRepository;
    private final PromotionMapper promotionMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<PromotionResponse> searchForManagement(String keyword, @NonNull Pageable pageable) {
        return searchForManagement(keyword, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PromotionResponse> searchForManagement(String keyword, String status, @NonNull Pageable pageable) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        String normalizedStatus = status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)
                ? null
                : status.trim().toUpperCase();
        LocalDateTime now = AppTime.now();

        return promotionRepository
                .searchForManagement(
                        normalizedKeyword, normalizedStatus, now.toLocalTime(), now.getDayOfWeek(), pageable)
                .map(promotionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PromotionResponse findById(@NonNull Long id) {
        return promotionMapper.toResponse(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public Promotion getEntityById(@NonNull Long id) {
        return promotionRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.PROMOTION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<Promotion> findActivePromotions() {
        LocalDateTime now = AppTime.now();
        LocalTime currentTime = now.toLocalTime();
        DayOfWeek currentDay = now.getDayOfWeek();

        return promotionRepository.findAllActive(currentTime, currentDay);
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> findActivePromotionResponses() {
        return promotionMapper.toResponses(findActivePromotions());
    }

    @Transactional(readOnly = true)
    public Optional<Promotion> findBestActivePromotion() {
        return findActivePromotions().stream()
                .filter(promotion -> promotion.getDiscountPercent() != null
                        && promotion.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.comparing(Promotion::getDiscountPercent));
    }

    @Transactional(readOnly = true)
    public BigDecimal calculatePromotionDiscount(BigDecimal subtotal) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        Optional<Promotion> bestPromotion = findBestActivePromotion();
        if (bestPromotion.isEmpty()) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal safeSubtotal = subtotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal discount = safeSubtotal
                .multiply(bestPromotion.get().getDiscountPercent())
                .divide(HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);

        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        return discount.min(safeSubtotal);
    }

    @Transactional
    @CacheEvict(value = CacheNames.PROMOTIONS, allEntries = true)
    public PromotionResponse create(@NonNull PromotionRequest request) {
        String normalizedName = normalizeName(request.name());

        if (promotionRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new BusinessException(ErrorCode.PROMOTION_NAME_EXISTS);
        }

        Promotion promotion = promotionMapper.toEntity(request, normalizedName);
        Promotion saved = promotionRepository.save(promotion);
        eventPublisher.publishEvent(new PromotionChangeEvent());
        return promotionMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = CacheNames.PROMOTIONS, allEntries = true)
    public PromotionResponse update(@NonNull Long id, @NonNull PromotionRequest request) {
        Promotion promotion = getEntityById(id);
        String normalizedName = normalizeName(request.name());

        if (promotionRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new BusinessException(ErrorCode.PROMOTION_NAME_EXISTS);
        }

        promotionMapper.updateEntity(promotion, request, normalizedName);
        Promotion saved = promotionRepository.save(promotion);
        eventPublisher.publishEvent(new PromotionChangeEvent());
        return promotionMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = CacheNames.PROMOTIONS, allEntries = true)
    public void delete(@NonNull Long id) {
        Promotion promotion = getEntityById(id);
        promotionRepository.delete(promotion);
        eventPublisher.publishEvent(new PromotionChangeEvent());
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        return name.trim();
    }
}
