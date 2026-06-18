package com.qros.modules.promotion.service;

import com.qros.modules.promotion.dto.request.VoucherRequest;
import com.qros.modules.promotion.dto.response.VoucherResponse;
import com.qros.modules.promotion.mapper.VoucherMapper;
import com.qros.modules.promotion.model.Voucher;
import com.qros.modules.promotion.repository.VoucherRepository;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.*;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
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
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherMapper voucherMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<VoucherResponse> searchForManagement(String keyword, String status, @NonNull Pageable pageable) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        String normalizedStatus = status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)
                ? null
                : status.trim().toUpperCase();

        return voucherRepository
                .searchForManagement(normalizedKeyword, normalizedStatus, AppTime.now(), pageable)
                .map(voucherMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public VoucherResponse findById(@NonNull Long id) {
        return voucherMapper.toResponse(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public Voucher getEntityById(@NonNull Long id) {
        return voucherRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND));
    }

    @Transactional
    @CacheEvict(value = CacheNames.VOUCHERS, allEntries = true)
    public VoucherResponse create(@NonNull VoucherRequest request) {
        String normalizedCode = normalizeCode(request.code());

        if (voucherRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new BusinessException(ErrorCode.VOUCHER_CODE_EXISTS);
        }

        Voucher voucher = voucherMapper.toEntity(request, normalizedCode);
        Voucher saved = voucherRepository.save(voucher);

        eventPublisher.publishEvent(new VoucherChangeEvent());

        return voucherMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = CacheNames.VOUCHERS, allEntries = true)
    public VoucherResponse update(@NonNull Long id, @NonNull VoucherRequest request) {
        Voucher voucher = getEntityById(id);
        String normalizedCode = normalizeCode(request.code());

        if (voucherRepository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, id)) {
            throw new BusinessException(ErrorCode.VOUCHER_CODE_EXISTS);
        }

        voucherMapper.updateEntity(voucher, request, normalizedCode);
        Voucher saved = voucherRepository.save(voucher);

        eventPublisher.publishEvent(new VoucherChangeEvent());

        return voucherMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = CacheNames.VOUCHERS, allEntries = true)
    public void delete(@NonNull Long id) {
        Voucher voucher = getEntityById(id);
        voucherRepository.delete(voucher);

        eventPublisher.publishEvent(new VoucherChangeEvent());
    }

    @Transactional
    @CacheEvict(value = CacheNames.VOUCHERS, allEntries = true)
    public void incrementUsage(@NonNull Long voucherId) {
        int updatedRows = voucherRepository.incrementUsedCountAtomically(voucherId);

        if (updatedRows != 1) {
            throw new BusinessException(ErrorCode.VOUCHER_USAGE_LIMIT_REACHED);
        }
    }

    @Transactional
    @CacheEvict(value = CacheNames.VOUCHERS, allEntries = true)
    public void incrementUsageByCode(String code) {
        String normalizedCode = normalizeNullableCode(code);

        if (normalizedCode == null) {
            return;
        }

        Voucher voucher = voucherRepository
                .findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND));

        incrementUsage(voucher.getId());
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        return code.trim().toUpperCase();
    }

    private String normalizeNullableCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        return code.trim().toUpperCase();
    }
}
