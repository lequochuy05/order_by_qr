package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.ApiResponse;
import com.sacmauquan.qrordering.dto.VoucherRequest;
import com.sacmauquan.qrordering.dto.VoucherValidateResponse;
import com.sacmauquan.qrordering.model.Voucher;
import com.sacmauquan.qrordering.service.DiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DiscountController - Manages vouchers and promotional discount codes.
 */
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountService service;

    /**
     * Retrieves a list of all vouchers in the system.
     * 
     * @return List of Voucher objects
     */
    @GetMapping
    public ApiResponse<List<Voucher>> list() {
        return ApiResponse.success(service.findAll());
    }

    /**
     * Retrieves detailed information of a specific voucher by its ID.
     * 
     * @param id Voucher ID
     * @return Found Voucher object
     */
    @GetMapping("/{id}")
    public ApiResponse<Voucher> get(@PathVariable @NonNull Long id) {
        return ApiResponse.success(service.findById(id));
    }

    /**
     * Creates a new voucher. The voucher code is automatically converted to
     * uppercase in the service layer.
     * 
     * @param req Voucher data for creation
     * @return Created Voucher object
     */
    @PostMapping
    public ApiResponse<Voucher> create(@Valid @RequestBody @NonNull VoucherRequest req) {
        return ApiResponse.success("Voucher created successfully", service.create(req));
    }

    /**
     * Updates an existing voucher's information.
     * 
     * @param id  Voucher ID to update
     * @param req Updated voucher data
     * @return Updated Voucher object
     */
    @PutMapping("/{id}")
    public ApiResponse<Voucher> update(@PathVariable @NonNull Long id,
            @Valid @RequestBody @NonNull VoucherRequest req) {
        return ApiResponse.success("Voucher updated successfully", service.update(id, req));
    }

    /**
     * Deletes a voucher from the system.
     * 
     * @param id Voucher ID to delete
     * @return Void success response
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @NonNull Long id) {
        service.delete(id);
        return ApiResponse.success("Voucher deleted successfully", null);
    }

    /**
     * Validates a voucher code and calculates the discount value based on the total
     * order amount.
     * 
     * @param code  The voucher code to validate
     * @param total The total order amount to apply the discount to
     * @return VoucherValidateResponse containing validity status and discount
     *         amount
     */
    @GetMapping("/validate")
    public ApiResponse<VoucherValidateResponse> validate(
            @RequestParam @NonNull String code,
            @RequestParam(defaultValue = "0") BigDecimal total) {
        return ApiResponse.success(service.validateCode(code, total));
    }
}
