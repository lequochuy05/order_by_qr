package com.qros.shared.security;

public final class PhoneMasker {

    private PhoneMasker() {}

    public static String mask(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return "****" + phone.substring(phone.length() - 4);
    }
}
