package com.qros.shared.constants;

public final class ApiRoutes {
    private ApiRoutes() {}

    public static final String PREFIX = "/api/v1";

    public static final String AUTH = PREFIX + "/auth";
    public static final String ORDERS = PREFIX + "/orders";
    public static final String ORDER_ITEMS = ORDERS + "/items";
    public static final String ORDER_BY_ID = ORDERS + "/{orderId}";
    public static final String TABLES = PREFIX + "/tables";
    public static final String MENU_ITEMS = PREFIX + "/menu-items";
    public static final String CATEGORIES = PREFIX + "/categories";
    public static final String KITCHEN = PREFIX + "/kitchen";
    public static final String PAYMENTS = PREFIX + "/payments";
    public static final String WEBHOOKS = PREFIX + "/webhooks";
    public static final String USERS = PREFIX + "/users";
    public static final String AI = PREFIX + "/ai";
    public static final String ANALYTICS = PREFIX + "/analytics";
    public static final String FORECAST = ANALYTICS + "/forecast";
    public static final String INVENTORY = PREFIX + "/inventory";
    public static final String INVENTORY_ITEMS = INVENTORY + "/items";
    public static final String INVENTORY_MOVEMENTS = INVENTORY + "/movements";
    public static final String INVENTORY_RECIPES = INVENTORY + "/recipes";
    public static final String COMBOS = PREFIX + "/combos";
    public static final String PROMOTIONS = PREFIX + "/promotions";
    public static final String VOUCHERS = PREFIX + "/vouchers";
    public static final String SETTINGS = PREFIX + "/settings";
    public static final String PUBLIC = PREFIX + "/public";
    public static final String PUBLIC_SETTINGS = PUBLIC + "/settings";
    public static final String RECOMMENDATIONS = PREFIX + "/recommendations";
}
