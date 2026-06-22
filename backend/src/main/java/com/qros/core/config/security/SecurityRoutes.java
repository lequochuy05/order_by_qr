package com.qros.core.config.security;

import com.qros.shared.constants.ApiRoutes;

public final class SecurityRoutes {

    private SecurityRoutes() {}

    public static final String[] STATIC_AND_SYSTEM = {"/error", "/ws/**", "/actuator/health", "/actuator/health/**"};

    public static final String[] AUTH_PUBLIC_POST = {
        ApiRoutes.AUTH + "/login",
        ApiRoutes.AUTH + "/refresh",
        ApiRoutes.AUTH + "/logout",
        ApiRoutes.AUTH + "/forgot-password-email",
        ApiRoutes.AUTH + "/reset-password-email",
        ApiRoutes.AUTH + "/forgot-password-phone",
        ApiRoutes.AUTH + "/reset-password-phone"
    };

    public static final String[] PUBLIC_GET = {
        ApiRoutes.PUBLIC + "/**", ApiRoutes.VOUCHERS + "/validate", ApiRoutes.RECOMMENDATIONS + "/**"
    };

    public static final String[] PUBLIC_POST = {
        ApiRoutes.PUBLIC + "/orders",
        ApiRoutes.PUBLIC + "/orders/preview",
        ApiRoutes.PUBLIC + "/tables/*/start-session",
        ApiRoutes.PUBLIC + "/sessions/heartbeat",
        ApiRoutes.PUBLIC + "/ai/chat",
        ApiRoutes.WEBHOOKS + "/**"
    };

    public static final String[] SELF_GET = {ApiRoutes.USERS + "/me"};

    public static final String[] SELF_PATCH = {ApiRoutes.USERS + "/me", ApiRoutes.USERS + "/me/password"};

    public static final String[] SELF_POST = {ApiRoutes.USERS + "/me/avatar"};

    public static final String[] MANAGER_GET = {
        ApiRoutes.USERS, ApiRoutes.USERS + "/*", ApiRoutes.SETTINGS, "/actuator/prometheus"
    };

    public static final String[] STAFF_READ_GET = {
        ApiRoutes.CATEGORIES + "/**",
        ApiRoutes.MENU_ITEMS + "/**",
        ApiRoutes.COMBOS + "/**",
        ApiRoutes.VOUCHERS + "/**",
        ApiRoutes.PROMOTIONS + "/**"
    };

    public static final String[] OPERATION_GET = {
        ApiRoutes.ORDERS,
        ApiRoutes.ORDERS + "/*",
        ApiRoutes.ORDERS + "/table-board",
        ApiRoutes.ORDERS + "/table/*/current",
        ApiRoutes.ORDERS + "/table/*/preview",
        ApiRoutes.TABLES + "/**",
        ApiRoutes.ORDERS + "/active",
        ApiRoutes.KITCHEN + "/orders",
        ApiRoutes.INVENTORY + "/**",
        ApiRoutes.FORECAST + "/**",
        ApiRoutes.ANALYTICS + "/**"
    };

    public static final String[] MANAGER_POST = {
        ApiRoutes.USERS,
        ApiRoutes.USERS + "/*/avatar",
        ApiRoutes.CATEGORIES + "/**",
        ApiRoutes.MENU_ITEMS + "/**",
        ApiRoutes.COMBOS + "/**",
        ApiRoutes.INVENTORY + "/**",
        ApiRoutes.TABLES + "/**",
        ApiRoutes.VOUCHERS + "/**",
        ApiRoutes.PROMOTIONS + "/**",
        ApiRoutes.AI + "/chat"
    };

    public static final String[] MANAGER_PUT = {
        ApiRoutes.USERS + "/**",
        ApiRoutes.CATEGORIES + "/**",
        ApiRoutes.MENU_ITEMS + "/**",
        ApiRoutes.COMBOS + "/**",
        ApiRoutes.INVENTORY + "/**",
        ApiRoutes.TABLES + "/**",
        ApiRoutes.VOUCHERS + "/**",
        ApiRoutes.PROMOTIONS + "/**",
        ApiRoutes.SETTINGS
    };

    public static final String[] MANAGER_PATCH = {
        ApiRoutes.CATEGORIES + "/**",
        ApiRoutes.MENU_ITEMS + "/**",
        ApiRoutes.TABLES + "/**",
        ApiRoutes.VOUCHERS + "/**",
        ApiRoutes.PROMOTIONS + "/**",
        ApiRoutes.USERS + "/*/reset-password",
        ApiRoutes.USERS + "/**",
        ApiRoutes.ORDERS + "/*/cancel",
        ApiRoutes.COMBOS + "/**"
    };

    public static final String[] MANAGER_DELETE = {
        ApiRoutes.ORDERS + "/**",
        ApiRoutes.USERS + "/**",
        ApiRoutes.CATEGORIES + "/**",
        ApiRoutes.MENU_ITEMS + "/**",
        ApiRoutes.INVENTORY + "/**",
        ApiRoutes.TABLES + "/**",
        ApiRoutes.VOUCHERS + "/**",
        ApiRoutes.PROMOTIONS + "/**",
        ApiRoutes.COMBOS + "/**"
    };

    public static final String[] KITCHEN_PATCH = {
        ApiRoutes.KITCHEN + "/items/*/status", ApiRoutes.KITCHEN + "/items/*/prepared"
    };

    public static final String[] STAFF_OPERATION_POST = {
        ApiRoutes.ORDERS, ApiRoutes.ORDERS + "/preview", ApiRoutes.ORDERS + "/*/reconcile"
    };

    public static final String[] STAFF_OPERATION_DELETE = {ApiRoutes.ORDER_ITEMS + "/*"};

    public static final String[] STAFF_OPERATION_PATCH = {ApiRoutes.ORDERS + "/*/status", ApiRoutes.ORDER_ITEMS + "/**"
    };

    public static final String[] PAYMENT_ROUTES = {ApiRoutes.PAYMENTS, ApiRoutes.PAYMENTS + "/**"};
}
