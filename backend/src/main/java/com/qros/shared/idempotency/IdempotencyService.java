package com.qros.shared.idempotency;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class IdempotencyService {

    private static final int REQUEST_TTL_SECONDS = 30;
    private static final int MAX_RECENT_REQUESTS = 100_000;

    private final Cache<String, Boolean> recentRequestIds = Caffeine.newBuilder()
            .expireAfterWrite(REQUEST_TTL_SECONDS, TimeUnit.SECONDS)
            .maximumSize(MAX_RECENT_REQUESTS)
            .build();

    public void requireNew(@NonNull String namespace, @NonNull String clientRequestId) {
        String normalizedId = normalize(clientRequestId);
        String key = namespace + ":" + normalizedId;

        Boolean previous = recentRequestIds.asMap().putIfAbsent(key, Boolean.TRUE);
        if (previous != null) {
            throw new BusinessException(
                    ErrorCode.ORDER_IDEMPOTENCY_CONFLICT,
                    "Yêu cầu đặt món này đã được gửi, vui lòng chờ hệ thống xử lý");
        }
    }

    private String normalize(String clientRequestId) {
        if (clientRequestId == null || clientRequestId.isBlank()) {
            throw new BusinessException(
                    ErrorCode.ORDER_IDEMPOTENCY_CONFLICT,
                    "Client request ID is required");
        }

        return clientRequestId.trim();
    }
}
