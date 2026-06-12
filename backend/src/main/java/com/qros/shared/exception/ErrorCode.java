package com.qros.shared.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    APP_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Application error"),
    BUSINESS_ERROR(HttpStatus.BAD_REQUEST, "Business rule violation"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Invalid input data"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Permission denied"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    CONFLICT(HttpStatus.CONFLICT, "Resource conflict"),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "Email not found"),
    PHONE_NOT_FOUND(HttpStatus.NOT_FOUND, "Phone number not found"),
    EMAIL_EXISTS(HttpStatus.CONFLICT, "Email already exists"),
    PHONE_EXISTS(HttpStatus.CONFLICT, "Phone number already exists"),
    ACCOUNT_INACTIVE(HttpStatus.FORBIDDEN, "Account is locked or not activated"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid login credentials"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid refresh token"),
    PASSWORD_INVALID(HttpStatus.BAD_REQUEST, "Current password is incorrect"),
    PASSWORD_RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "Token invalid or expired"),

    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Category not found"),
    CATEGORY_NAME_EXISTS(HttpStatus.CONFLICT, "Category name already exists"),
    MENU_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "Menu item not found"),
    MENU_ITEM_NAME_EXISTS(HttpStatus.CONFLICT, "Item name already exists"),
    COMBO_NOT_FOUND(HttpStatus.NOT_FOUND, "Combo not found"),
    COMBO_NAME_EXISTS(HttpStatus.CONFLICT, "Combo name already exists"),

    TABLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Table not found"),
    TABLE_CODE_INVALID(HttpStatus.NOT_FOUND, "Invalid table code"),
    TABLE_NUMBER_EXISTS(HttpStatus.CONFLICT, "Table number already exists"),
    TABLE_QR_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "System error generating QR code"),

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Order not found"),
    ORDER_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "Order item not found"),
    ORDER_CONTENT_EMPTY(HttpStatus.BAD_REQUEST, "Order content cannot be empty"),
    ORDER_INVALID_STATUS(HttpStatus.BAD_REQUEST, "Invalid order status"),
    ORDER_INVALID_ITEM_STATUS(HttpStatus.BAD_REQUEST, "Invalid item status"),
    ORDER_INVALID_STATE(HttpStatus.BAD_REQUEST, "Invalid order state"),
    ORDER_ALREADY_PAID(HttpStatus.BAD_REQUEST, "Order is already paid"),
    ORDER_PAYMENT_INVALID(HttpStatus.BAD_REQUEST, "Order payment is invalid"),

    VOUCHER_NOT_FOUND(HttpStatus.NOT_FOUND, "Voucher not found"),
    VOUCHER_CODE_EXISTS(HttpStatus.CONFLICT, "Voucher code already exists"),
    VOUCHER_INACTIVE(HttpStatus.BAD_REQUEST, "Voucher is inactive"),
    VOUCHER_NOT_YET_ACTIVE(HttpStatus.BAD_REQUEST, "Voucher is not active yet"),
    VOUCHER_EXPIRED(HttpStatus.BAD_REQUEST, "Voucher has expired"),
    VOUCHER_USAGE_LIMIT_REACHED(HttpStatus.BAD_REQUEST, "Voucher usage limit has been reached"),
    VOUCHER_INVALID(HttpStatus.BAD_REQUEST, "Voucher invalid"),

    PROMOTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Promotion not found"),
    PROMOTION_NAME_EXISTS(HttpStatus.CONFLICT, "Promotion name already exists"),
    PROMOTION_INACTIVE(HttpStatus.BAD_REQUEST, "Promotion is inactive"),
    PROMOTION_INVALID(HttpStatus.BAD_REQUEST, "Promotion invalid"),

    INVENTORY_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "Inventory item not found"),
    INVENTORY_ITEM_NAME_EXISTS(HttpStatus.CONFLICT, "Inventory item name already exists"),
    INVENTORY_ITEM_IN_USE(HttpStatus.CONFLICT, "Inventory item is being used in recipe"),
    INVENTORY_QUANTITY_INVALID(HttpStatus.BAD_REQUEST, "Inventory quantity is invalid"),
    INVENTORY_QUANTITY_BELOW_RESERVED(HttpStatus.BAD_REQUEST,
            "Quantity on hand cannot be lower than reserved quantity"),
    INVENTORY_INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "Insufficient inventory stock"),
    RECIPE_ITEM_DUPLICATED(HttpStatus.BAD_REQUEST, "Recipe item is duplicated"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "From date cannot be after to date"),

    PAYMENT_TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Transaction not found"),
    PAYMENT_GATEWAY_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Payment gateway error"),
    PAYMENT_CANCELLATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Payment cancellation failed"),
    PAYMENT_TRANSACTION_INVALID_STATE(HttpStatus.BAD_REQUEST, "Invalid payment transaction state"),
    PAYMENT_IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "Payment idempotency conflict"),
    PAYMENT_WEBHOOK_INVALID(HttpStatus.BAD_REQUEST, "Invalid payment webhook"),

    FILE_INVALID(HttpStatus.BAD_REQUEST, "Invalid file"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to upload file");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
