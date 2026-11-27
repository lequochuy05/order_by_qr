package com.qros.modules.user.listener;

import com.qros.shared.cache.CacheHelper;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.UserAuthChangeEvent;
import com.qros.shared.event.DomainEvents.UserChangeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserCacheInvalidationListener {

    private final CacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserChange(UserChangeEvent event) {
        CacheHelper.clear(cacheManager, CacheNames.USERS);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserAuthChange(UserAuthChangeEvent event) {
        if (event == null) {
            return;
        }
        evictAuthKey(event.email());
        evictAuthKey(event.previousEmail());
    }

    private void evictAuthKey(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        CacheHelper.evict(cacheManager, CacheNames.USER_AUTH, email.trim().toLowerCase());
    }
}
