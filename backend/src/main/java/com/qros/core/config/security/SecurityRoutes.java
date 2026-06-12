package com.qros.core.config.security;

public final class SecurityRoutes {

    private SecurityRoutes() {}

    public static final String[] STATIC_AND_SYSTEM = {
            "/error",
            "/ws/**",
            "/actuator/health",
            "/actuator/prometheus"
    };

    public static final String[] AUTH_PUBLIC_POST = {
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/forgot-password-email",
            "/api/auth/reset-password-email",
            "/api/auth/forgot-password-phone",
            "/api/auth/reset-password-phone"
    };

    public static final String[] PUBLIC_GET = {
            "/api/public/**",
            "/api/vouchers/validate",
            "/api/recommendations/**"
    };

    public static final String[] PUBLIC_POST = {
            "/api/public/orders",
            "/api/webhooks/**"
    };

    public static final String[] AUTHENTICATED_POST = {
            "/api/ai/chat"
    };

    public static final String[] SELF_GET = {
            "/api/users/me"
    };

    public static final String[] SELF_PATCH = {
            "/api/users/me",
            "/api/users/me/password"
    };

    public static final String[] SELF_POST = {
            "/api/users/me/avatar"
    };

    public static final String[] MANAGER_GET = {
            "/api/users",
            "/api/users/*",
            "/api/settings"
    };

    public static final String[] STAFF_READ_GET = {
            "/api/categories/**",
            "/api/menu-items/**",
            "/api/combos/**",
            "/api/vouchers/**",
            "/api/promotions/**"
    };

    public static final String[] OPERATION_GET = {
            "/api/orders",
            "/api/orders/table-board",
            "/api/orders/table/*/current",
            "/api/tables/**",
            "/api/orders/history",
            "/api/orders/stats",
            "/api/orders/active",
            "/api/kitchen/orders",
            "/api/inventory/**",
            "/api/stats/**",
            "/api/analytics/**"
    };

    public static final String[] MANAGER_POST = {
            "/api/users",
            "/api/users/*/avatar",
            "/api/categories/**",
            "/api/menu-items/**",
            "/api/combos/**",
            "/api/inventory/**",
            "/api/tables/**",
            "/api/vouchers/**",
            "/api/promotions/**",
            "/api/admin/ai/**"
    };

    public static final String[] MANAGER_PUT = {
            "/api/categories/**",
            "/api/menu-items/**",
            "/api/combos/**",
            "/api/inventory/**",
            "/api/vouchers/**",
            "/api/promotions/**",
            "/api/settings"
    };

    public static final String[] MANAGER_PATCH = {
            "/api/categories/**",
            "/api/menu-items/**",
            "/api/tables/**",
            "/api/vouchers/**",
            "/api/promotions/**",
            "/api/users/*/reset-password",
            "/api/users/**",
            "/api/orders/**",
            "/api/combos/**"
    };

    public static final String[] MANAGER_DELETE = {
            "/api/orders/**",
            "/api/users/**",
            "/api/categories/**",
            "/api/menu-items/**",
            "/api/inventory/**",
            "/api/tables/**",
            "/api/vouchers/**",
            "/api/promotions/**",
            "/api/combos/**",
            "/api/combos/*/items",
            "/api/settings/**"
    };

    public static final String[] KITCHEN_PATCH = {
            "/api/kitchen/items/*/status",
            "/api/kitchen/items/*/prepared"
    };

    public static final String[] STAFF_OPERATION_POST = {
            "/api/orders",
            "/api/orders/preview",
            "/api/orders/*/pay",
            "/api/orders/*/confirm-paid",
            "/api/orders/*/reconcile"
    };

    public static final String[] STAFF_OPERATION_DELETE = {
            "/api/orders/items/*"
    };

    public static final String[] STAFF_OPERATION_PATCH = {
            "/api/orders/items/**"
    };

    public static final String[] PAYMENT_ROUTES = {
            "/api/payments/**"
    };
}
