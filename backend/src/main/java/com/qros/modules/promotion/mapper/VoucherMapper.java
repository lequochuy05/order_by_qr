package com.qros.modules.promotion.mapper;

import com.qros.modules.promotion.dto.internal.DiscountResult;
import com.qros.modules.promotion.dto.request.VoucherRequest;
import com.qros.modules.promotion.dto.response.VoucherResponse;
import com.qros.modules.promotion.dto.response.VoucherValidateResponse;
import com.qros.modules.promotion.model.Voucher;
import com.qros.modules.promotion.model.enums.VoucherValidationStatus;
import com.qros.modules.promotion.repository.projection.VoucherValidationProjection;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class VoucherMapper {

    public Voucher toEntity(VoucherRequest request, String normalizedCode) {
        return Voucher.builder()
                .code(normalizedCode)
                .type(request.type())
                .discountAmount(request.discountAmount())
                .discountPercent(request.discountPercent())
                .usageLimit(request.usageLimit())
                .usedCount(0)
                .validFrom(request.validFrom())
                .validTo(request.validTo())
                .active(request.active() != null ? request.active() : true)
                .build();
    }

    public void updateEntity(Voucher voucher, VoucherRequest request, String normalizedCode) {
        voucher.setCode(normalizedCode);
        voucher.setType(request.type());
        voucher.setDiscountAmount(request.discountAmount());
        voucher.setDiscountPercent(request.discountPercent());
        voucher.setUsageLimit(request.usageLimit());
        voucher.setValidFrom(request.validFrom());
        voucher.setValidTo(request.validTo());
        voucher.setActive(request.active() != null ? request.active() : voucher.getActive());
    }

    public VoucherResponse toResponse(Voucher voucher) {
        return new VoucherResponse(
                voucher.getId(),
                voucher.getCode(),
                voucher.getType(),
                voucher.getDiscountAmount(),
                voucher.getDiscountPercent(),
                voucher.getUsageLimit(),
                voucher.getUsedCount(),
                voucher.getValidFrom(),
                voucher.getValidTo(),
                voucher.getActive());
    }

    public List<VoucherResponse> toResponses(List<Voucher> vouchers) {
        return vouchers.stream().map(this::toResponse).toList();
    }

    public VoucherValidateResponse toValidateResponse(
            VoucherValidationProjection voucher,
            VoucherValidationStatus status,
            DiscountResult discountResult,
            boolean applicable) {
        return new VoucherValidateResponse(
                voucher.getCode(),
                status,
                voucher.getType(),
                voucher.getDiscountAmount(),
                voucher.getDiscountPercent(),
                discountResult.appliedDiscountAmount(),
                discountResult.finalAmount(),
                applicable);
    }

    public VoucherValidateResponse notFoundValidateResponse(String code, BigDecimal subtotal) {
        return new VoucherValidateResponse(
                code,
                VoucherValidationStatus.NOT_FOUND,
                null,
                null,
                null,
                BigDecimal.ZERO,
                subtotal != null ? subtotal : BigDecimal.ZERO,
                false);
    }

    public VoucherValidateResponse notApplicableValidateResponse(
            VoucherValidationProjection voucher, VoucherValidationStatus status, DiscountResult discountResult) {
        return new VoucherValidateResponse(
                voucher.getCode(),
                status,
                voucher.getType(),
                voucher.getDiscountAmount(),
                voucher.getDiscountPercent(),
                discountResult.appliedDiscountAmount(),
                discountResult.finalAmount(),
                false);
    }
}
