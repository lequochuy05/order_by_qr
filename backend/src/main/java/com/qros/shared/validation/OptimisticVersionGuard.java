package com.qros.shared.validation;

import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.util.Objects;

public final class OptimisticVersionGuard {

    private OptimisticVersionGuard() {}

    public static void requireMatch(
            Long currentVersion, Long expectedVersion, String entityDisplayName, String entityLowerName) {
        if (expectedVersion == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    entityDisplayName + " version is required. Please refresh and try again.");
        }

        matchIfPresent(currentVersion, expectedVersion, entityLowerName);
    }

    public static void matchIfPresent(Long currentVersion, Long expectedVersion, String entityLowerName) {
        if (expectedVersion == null) {
            return;
        }

        if (!Objects.equals(currentVersion, expectedVersion)) {
            throw new BusinessException(
                    ErrorCode.CONCURRENT_MODIFICATION,
                    "This " + entityLowerName + " was changed by another request. Please refresh and try again.");
        }
    }
}
