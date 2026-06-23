package com.qros.modules.auth.store;

public interface RefreshTokenStore {

    void create(String key, String value, long ttlMs);

    String get(String key);

    boolean consumeAtomically(String key);

    void revoke(String key);
}
