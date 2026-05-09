package com.sacmauquan.qrordering.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sacmauquan.qrordering.dto.ApiResponse;
import com.sacmauquan.qrordering.dto.OrderRequest;
import com.sacmauquan.qrordering.dto.OrderResponse;
import com.sacmauquan.qrordering.dto.OrderPreviewResponse;
import com.sacmauquan.qrordering.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * OrderController - Quản lý quy trình gọi món, xử lý hóa đơn và KDS.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Tạo đơn hàng mới
     */
    @PostMapping
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody @NonNull OrderRequest orderRequest) {
        return ApiResponse.success("Đặt món thành công", orderService.createOrder(orderRequest));
    }

    /**
     * Lấy toàn bộ danh sách đơn hàng
     */
    @GetMapping
    public ApiResponse<List<OrderResponse>> getAllOrders() {
        return ApiResponse.success(orderService.getAllOrders());
    }

    /**
     * Tra cứu lịch sử đơn hàng với bộ lọc nâng cao và phân trang
     */
    @GetMapping("/history")
    public ApiResponse<Page<OrderResponse>> getOrderHistory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 15, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(orderService.getOrderHistory(status, startDate, endDate, search, pageable));
    }

    /**
     * Lấy báo cáo thống kê đơn hàng theo bộ lọc thời gian
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getOrderStats(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        return ApiResponse.success(orderService.getOrderStats(status, startDate, endDate));
    }

    /**
     * Cập nhật trạng thái tổng quát của đơn hàng
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(
            @PathVariable @NonNull Long id,
            @RequestBody @NonNull Map<String, String> body) {
        String status = body.get("status");
        return ApiResponse.success("Cập nhật trạng thái thành công", orderService.updateStatus(id, status));
    }

    /**
     * Đánh dấu món ăn trong đơn hàng đã hoàn thành chế biến (KDS)
     */
    @PatchMapping("/items/{itemId}/prepared")
    public ApiResponse<Void> markItemPrepared(@PathVariable @NonNull Long itemId) {
        orderService.updateItemStatus(itemId, "FINISHED");
        return ApiResponse.success("Đã xác nhận hoàn thành món", null);
    }

    /**
     * Cập nhật trạng thái chi tiết của từng món ăn
     */
    @PatchMapping("/items/{itemId}/status")
    public ApiResponse<Void> updateItemStatus(
            @PathVariable @NonNull Long itemId,
            @RequestBody @NonNull Map<String, String> body) {
        String status = body.get("status");
        orderService.updateItemStatus(itemId, status);
        return ApiResponse.success("Đã cập nhật trạng thái món: " + status, null);
    }

    /**
     * Lấy danh sách các đơn hàng đang chờ chế biến (KDS)
     */
    @GetMapping("/kitchen")
    public ApiResponse<List<OrderResponse>> getKitchenOrders() {
        return ApiResponse.success(orderService.getKitchenOrders());
    }

    /**
     * Lấy đơn hàng hiện tại của một bàn
     */
    @GetMapping("/table/{tableId}/current")
    public ApiResponse<OrderResponse> getCurrentOrderByTable(@PathVariable @NonNull Long tableId) {
        return orderService.getCurrentOrderByTable(tableId)
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.success(null));
    }

    /**
     * Lấy danh sách đơn hàng đang hoạt động
     */
    @GetMapping("/active")
    public ApiResponse<List<OrderResponse>> getActiveOrders() {
        return ApiResponse.success(orderService.getActiveOrders());
    }

    /**
     * Cập nhật số lượng hoặc ghi chú của một món ăn trong đơn
     */
    @PatchMapping("/items/{itemId}")
    public ApiResponse<OrderResponse> updateOrderItem(
            @PathVariable @NonNull Long itemId,
            @RequestBody @NonNull Map<String, Object> body) {
        int qty = (int) body.getOrDefault("quantity", 1);
        String notes = (String) body.getOrDefault("notes", "");
        return ApiResponse.success("Cập nhật món ăn thành công",
                orderService.updateOrderItem(itemId, qty, notes));
    }

    /**
     * Thực hiện thanh toán và áp dụng Voucher giảm giá
     */
    @PatchMapping("/{orderId}/pay")
    public ApiResponse<String> payOrder(
            @PathVariable @NonNull Long orderId,
            @RequestParam @NonNull Long userId,
            @RequestParam(required = false) String voucherCode) {
        String result = orderService.payOrder(orderId, userId, voucherCode);
        return ApiResponse.success("Thanh toán thành công", result);
    }

    /**
     * Hủy một món ăn khỏi đơn hàng
     */
    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> cancelOrderItem(@PathVariable @NonNull Long itemId) {
        orderService.cancelOrderItem(itemId);
        return ApiResponse.success("Đã hủy món ăn thành công", null);
    }

    /**
     * Tính toán thử tổng tiền đơn hàng trước khi xác nhận
     */
    @PostMapping("/preview")
    public ApiResponse<OrderPreviewResponse> preview(@Valid @RequestBody @NonNull OrderRequest req) {
        return ApiResponse.success(orderService.preview(req));
    }
}
