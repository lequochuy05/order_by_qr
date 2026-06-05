package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.OrderPreviewResponse;
import com.sacmauquan.qrordering.dto.OrderRequest;
import com.sacmauquan.qrordering.dto.OrderResponse;
import com.sacmauquan.qrordering.dto.CustomerPublicDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OrderService - Core interface for managing the complete ordering lifecycle.
 * Handles customer requests, kitchen preparation states, and financial
 * transactions.
 */
public interface OrderService {

    /**
     * Retrieves all orders currently registered in the system.
     * 
     * @return List of OrderResponse objects
     */
    List<OrderResponse> getAllOrders();

    /**
     * Retrieves filtered and paginated order history records.
     * 
     * @param status        Status filter
     * @param startDate     Range start
     * @param endDate       Range end
     * @param orderId       Order ID filter
     * @param tableNumber   Table number filter
     * @param pageable      Pagination configuration
     * @return Page of OrderResponse objects
     */
    Page<OrderResponse> getOrderHistory(String status, LocalDate startDate, LocalDate endDate, String orderId,
            String tableNumber, @NonNull Pageable pageable);

    /**
     * Generates high-level statistical summaries for specified order criteria.
     * 
     * @return Map containing metrics like total revenue and volume
     */
    Map<String, Object> getOrderStats(String status, LocalDate startDate, LocalDate endDate, String orderId,
            String tableNumber);

    /**
     * Synchronizes order status with external payment providers or internal rules.
     * 
     * @param id Order ID
     * @return Updated OrderResponse
     */
    OrderResponse reconcileOrder(@NonNull Long id);

    /**
     * Transitions an order between lifecycle states (e.g., PENDING to SERVING).
     * 
     * @param id     Order ID
     * @param status Target status string
     * @return Updated OrderResponse
     */
    OrderResponse updateStatus(@NonNull Long id, @NonNull String status);

    /**
     * Creates a new order or merges items into an existing session for the same
     * table.
     * 
     * @param req Order request data
     * @return Created or updated OrderResponse
     */
    OrderResponse createOrder(@NonNull OrderRequest req);

    /**
     * Cancels a specific item line from an active order.
     * 
     * @param itemId Item identifier
     */
    void cancelOrderItem(@NonNull Long itemId);

    /**
     * Updates the preparation status of a specific item within the kitchen.
     * 
     * @param itemId    Item identifier
     * @param newStatus preparation status (e.g., COOKING)
     */
    void updateItemStatus(@NonNull Long itemId, @NonNull String newStatus);

    /**
     * Marks an item as ready for service.
     */
    void markItemPrepared(@NonNull Long itemId);

    /**
     * Retrieves orders that require immediate attention in the kitchen.
     */
    List<OrderResponse> getKitchenOrders();

    /**
     * Locates the active ordering session for a specific dining table.
     */
    Optional<OrderResponse> getCurrentOrderByTable(@NonNull Long tableId);

    Optional<CustomerPublicDto.Order> getPublicCurrentOrderByTable(@NonNull Long tableId);

    /**
     * Updates line-item specifics like quantity or custom instructions.
     */
    OrderResponse updateOrderItem(@NonNull Long itemId, int quantity, String notes);

    /**
     * Finalizes the financial transaction for an order (Cash payment flow).
     * 
     * @param id          Order ID
     * @param userId      The staff member processing the payment
     * @param voucherCode Optional promotional voucher
     * @return Success confirmation string
     */
    String payOrder(@NonNull Long id, @NonNull Long userId, String voucherCode);

    /**
     * Calculates the projected bill for a potential order without persisting it.
     */
    OrderPreviewResponse preview(@NonNull OrderRequest req);

    /**
     * Lists all orders currently in progress (not completed or cancelled).
     */
    List<OrderResponse> getActiveOrders();

    /**
     * Confirms that an order has been settled financially.
     */
    OrderResponse confirmPaid(@NonNull Long id);

    /**
     * Terminates an order session entirely.
     */
    OrderResponse cancelOrder(@NonNull Long id);

    /**
     * Retrieves a detailed view of a single order.
     */
    OrderResponse getOrderById(@NonNull Long id);

    /**
     * Generates a billing preview for the current active order on a table.
     */
    OrderPreviewResponse getOrderPreviewByTableId(@NonNull Long tableId);

    /**
     * Permanently removes an order record from system history.
     */
    void deleteOrder(@NonNull Long id);
}
