package com.qros.shared.security;

import java.time.LocalDateTime;

public interface PasswordVersionedUserDetails {

    LocalDateTime getPasswordChangedAt();
}
