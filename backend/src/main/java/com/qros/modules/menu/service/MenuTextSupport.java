package com.qros.modules.menu.service;

import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;

final class MenuTextSupport {

    private MenuTextSupport() {}

    static String normalizeRequired(String value, String message) {
        String normalized = normalizeBlank(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
        return normalized;
    }

    static String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static boolean isHttpImage(String img) {
        return img != null && !img.isBlank() && img.startsWith("http");
    }
}
