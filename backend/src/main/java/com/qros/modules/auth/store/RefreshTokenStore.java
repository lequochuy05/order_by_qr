package com.qros.modules.auth.store;

public interface RefreshTokenStore {

    void create(String key, String value, long ttlMs);

    String get(String key);

    boolean consumeAtomically(String key, String expectedValue);

    void revoke(String key);

    default void createSession(String userIndexKey, String sessionKey, String value, long ttlMs) {
        create(sessionKey, value, ttlMs);
    }

    default boolean consumeSessionAtomically(String userIndexKey, String sessionKey, String expectedValue) {
        return consumeAtomically(sessionKey, expectedValue);
    }

    default void revokeSession(String userIndexKey, String sessionKey) {
        revoke(sessionKey);
    }

    default void revokeAllUserSessions(String userIndexKey, String legacyKey) {
        revoke(legacyKey);
    }
}
