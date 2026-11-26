package com.qros.shared.security;

import com.qros.shared.time.AppTime;
import java.time.LocalDateTime;
import java.util.Date;
import org.springframework.security.core.userdetails.UserDetails;

public final class UserAccountValidator {

    private UserAccountValidator() {}

    public static boolean isUsableAccount(UserDetails userDetails) {
        return userDetails != null
                && userDetails.isEnabled()
                && userDetails.isAccountNonLocked()
                && userDetails.isAccountNonExpired()
                && userDetails.isCredentialsNonExpired();
    }

    public static boolean isFreshForPasswordVersion(String token, UserDetails userDetails, JwtService jwtService) {
        if (!(userDetails instanceof PasswordVersionedUserDetails passwordVersionedUser)) {
            return true;
        }

        LocalDateTime passwordChangedAt = passwordVersionedUser.getPasswordChangedAt();
        if (passwordChangedAt == null) {
            return true;
        }

        Date issuedAt = jwtService.extractIssuedAt(token);
        if (issuedAt == null) {
            return false;
        }

        long issuedAtSec = issuedAt.getTime() / 1000;
        long passwordChangedAtSec = passwordChangedAt.atZone(AppTime.ZONE).toEpochSecond();
        return issuedAtSec >= passwordChangedAtSec;
    }
}
