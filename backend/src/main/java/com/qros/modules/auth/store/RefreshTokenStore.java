package com.qros.modules.auth.store;

public interface RefreshTokenStore {

    void create(String key, long ttlMs);

    boolean consumeAtomically(String key);

    void revoke(String key);
}
