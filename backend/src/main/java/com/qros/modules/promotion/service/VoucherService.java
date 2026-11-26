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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherMapper voucherMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final VoucherUsageCheckPort voucherUsageCheckPort;

    public VoucherService(
            VoucherRepository voucherRepository,
            VoucherMapper voucherMapper,
            ApplicationEventPublisher eventPublisher) {
        this(voucherRepository, voucherMapper, eventPublisher, null);
    }

    @Autowired
    public VoucherService(
            VoucherRepository voucherRepository,
            VoucherMapper voucherMapper,
            ApplicationEventPublisher eventPublisher,
            VoucherUsageCheckPort voucherUsageCheckPort) {
        this.voucherRepository = voucherRepository;
        this.voucherMapper = voucherMapper;
        this.eventPublisher = eventPublisher;
        this.voucherUsageCheckPort = voucherUsageCheckPort;
    }

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

        boolean renamingCode = !voucher.getCode().equalsIgnoreCase(normalizedCode);
        if (renamingCode && isInActiveUse(voucher)) {
            throw new BusinessException(
                    ErrorCode.VOUCHER_IN_USE,
                    "Cannot rename voucher code while it is referenced by active orders or pending payments");
        }

        int committedUsage = safeInt(voucher.getUsedCount()) + safeInt(voucher.getReservedCount());
        if (request.usageLimit() != null && request.usageLimit() > 0 && request.usageLimit() < committedUsage) {
            throw new BusinessException(
                    ErrorCode.VOUCHER_IN_USE,
                    "Usage limit cannot be lower than current used and reserved count (" + committedUsage + ")");
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
        if (isInActiveUse(voucher)) {
            throw new BusinessException(
                    ErrorCode.VOUCHER_IN_USE,
                    "Cannot delete voucher while it is referenced by active orders or pending payments");
        }
        voucherRepository.delete(voucher);

        eventPublisher.publishEvent(new VoucherChangeEvent());
    }

    @Transactional
    @CacheEvict(value = CacheNames.VOUCHERS, allEntries = true)
    public boolean tryReserveUsageByCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        Voucher voucher = voucherRepository
                .findByCodeIgnoreCaseForUpdate(code.trim().toUpperCase())
                .orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND));
        return voucherRepository.reserveUsageAtomically(voucher.getId()) == 1;
    }

    @Transactional
    @CacheEvict(value = CacheNames.VOUCHERS, allEntries = true)
    public void releaseReservedUsageByCode(String code) {
        if (code == null || code.isBlank()) {
            return;
        }
        voucherRepository.releaseReservedUsageAtomically(code.trim().toUpperCase());
    }

    @Transactional
    @CacheEvict(value = CacheNames.VOUCHERS, allEntries = true)
    public boolean tryConfirmReservedUsage(@NonNull Long voucherId) {
        return voucherRepository.confirmReservedUsageAtomically(voucherId) == 1;
    }

    @Transactional
    @CacheEvict(value = CacheNames.VOUCHERS, allEntries = true)
    public boolean tryIncrementUsage(@NonNull Long voucherId) {
        return voucherRepository.incrementUsedCountAtomically(voucherId) == 1;
    }

    @Transactional
    @CacheEvict(value = CacheNames.VOUCHERS, allEntries = true)
    public void incrementUsage(@NonNull Long voucherId) {
        if (!tryIncrementUsage(voucherId)) {
            throw new BusinessException(ErrorCode.VOUCHER_USAGE_LIMIT_REACHED);
        }
    }

    private boolean isInActiveUse(Voucher voucher) {
        return safeInt(voucher.getReservedCount()) > 0
                || (voucherUsageCheckPort != null && voucherUsageCheckPort.isVoucherInActiveUse(voucher.getCode()));
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        return code.trim().toUpperCase();
    }
}
