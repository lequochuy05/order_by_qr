package com.qros.modules.auth.service;

import com.qros.infrastructure.mail.EmailService;
import com.qros.infrastructure.sms.SmsService;
import com.qros.modules.auth.config.PasswordResetProperties;
import com.qros.modules.auth.model.PasswordResetToken;
import com.qros.modules.auth.repository.PasswordResetTokenRepository;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserStatus;
import com.qros.modules.user.service.UserLookupService;
import com.qros.shared.event.DomainEvents.UserAuthChangeEvent;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import com.qros.shared.transaction.TransactionSideEffectService;
import java.security.SecureRandom;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PasswordResetService - Manages secure password recovery flows via Email
 * (token-based) and SMS (OTP-based).
 */
@Slf4j
@Service
public class PasswordResetService {

    private final UserLookupService userRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final EmailService emailService;
    private final SmsService smsService;
    private final PasswordResetProperties passwordResetProperties;
    private final Environment environment;

    private final PasswordEncoder passwordEncoder;
    private final TransactionSideEffectService sideEffects;
    private final RefreshTokenService refreshTokenService;
    private final AccountLockoutService accountLockoutService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordResetAttemptRecorder attemptRecorder;

    public PasswordResetService(
            UserLookupService userRepo,
            PasswordResetTokenRepository tokenRepo,
            EmailService emailService,
            SmsService smsService,
            PasswordResetProperties passwordResetProperties,
            Environment environment,
            PasswordEncoder passwordEncoder,
            TransactionSideEffectService sideEffects,
            RefreshTokenService refreshTokenService,
            AccountLockoutService accountLockoutService,
            ApplicationEventPublisher eventPublisher) {
        this(
                userRepo,
                tokenRepo,
                emailService,
                smsService,
                passwordResetProperties,
                environment,
                passwordEncoder,
                sideEffects,
                refreshTokenService,
                accountLockoutService,
                eventPublisher,
                null);
    }

    @Autowired
    public PasswordResetService(
            UserLookupService userRepo,
            PasswordResetTokenRepository tokenRepo,
            EmailService emailService,
            SmsService smsService,
            PasswordResetProperties passwordResetProperties,
            Environment environment,
            PasswordEncoder passwordEncoder,
            TransactionSideEffectService sideEffects,
            RefreshTokenService refreshTokenService,
            AccountLockoutService accountLockoutService,
            ApplicationEventPublisher eventPublisher,
            PasswordResetAttemptRecorder attemptRecorder) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
        this.emailService = emailService;
        this.smsService = smsService;
        this.passwordResetProperties = passwordResetProperties;
        this.environment = environment;
        this.passwordEncoder = passwordEncoder;
        this.sideEffects = sideEffects;
        this.refreshTokenService = refreshTokenService;
        this.accountLockoutService = accountLockoutService;
        this.eventPublisher = eventPublisher;
        this.attemptRecorder = attemptRecorder;
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Initiates a password reset flow by generating a unique token and sending it
     * via email.
     *
     * @param email The registered email address
     */
    @Transactional
    public void createPasswordResetToken(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepo.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_FOUND));
        if (!isAccountActive(user)) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_FOUND);
        }

        String rawToken = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        tokenRepo.markAllActiveTokensUsedByUserId(user.getId());

        resetToken.setUser(user);
        resetToken.setToken(sha256(rawToken));
        resetToken.setExpiryDate(AppTime.now().plusMinutes(30));
        resetToken.setVia(PasswordResetToken.Via.EMAIL);
        resetToken.setUsed(false);

        tokenRepo.save(resetToken);

        sideEffects.afterCommit(
                () -> emailService.sendResetPasswordEmail(normalizedEmail, rawToken), "send reset password email");
    }

    /**
     * Completes the password reset process using a valid security token.
     *
     * @param token  The reset token from email
     * @param newPassword The new plain text password
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepo
                .findByTokenForUpdate(sha256(token))
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID, "Token invalid or expired"));

        if (resetToken.isUsed() || resetToken.getExpiryDate().isBefore(AppTime.now())) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID, "Token expired or already used");
        }

        User user = resetToken.getUser();
        if (!isAccountActive(user)) {
            throw new BusinessException(
                    ErrorCode.ACCOUNT_INACTIVE, "Inactive or banned accounts cannot reset password");
        }
        resetToken.setUsed(true);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(AppTime.now().truncatedTo(ChronoUnit.SECONDS));
        user.setMustChangePassword(false);
        userRepo.save(user);
        tokenRepo.save(resetToken);
        refreshTokenService.revokeUserTokens(user.getId());
        accountLockoutService.unlock(user.getEmail());
        eventPublisher.publishEvent(new UserAuthChangeEvent(user.getEmail(), null));
    }

    /**
     * Initiates a mobile-based password reset by generating and sending a 6-digit
     * OTP.
     *
     * @param phone The registered phone number
     */
    @Transactional
    public void createOtpAndSendOtp(String phone) {
        if (!passwordResetProperties.isPhoneEnabled()) {
            throw new BusinessException(ErrorCode.FEATURE_DISABLED, "Phone password reset is currently disabled");
        }

        String normalizedPhone = normalizePhone(phone);

        User user = userRepo.findByPhone(normalizedPhone)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_NOT_FOUND));
        if (!isAccountActive(user)) {
            throw new BusinessException(ErrorCode.PHONE_NOT_FOUND);
        }
        boolean logOtpInDev = shouldLogOtpInDev();
        if (!logOtpInDev && !smsService.isAvailable()) {
            throw new BusinessException(ErrorCode.FEATURE_DISABLED, "SMS delivery is not configured");
        }

        tokenRepo.markAllActiveTokensUsedByUserId(user.getId());

        String rawOtpCode = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));

        PasswordResetToken otpToken = new PasswordResetToken();
        otpToken.setUser(user);
        otpToken.setOtpCode(sha256(rawOtpCode));
        otpToken.setToken(sha256(UUID.randomUUID().toString()));
        otpToken.setExpiryDate(AppTime.now().plusMinutes(5));
        otpToken.setVia(PasswordResetToken.Via.PHONE);
        otpToken.setUsed(false);

        tokenRepo.save(otpToken);

        if (logOtpInDev) {
            log.warn("[DEV ONLY] Password reset OTP for phone {} is {}", maskPhone(normalizedPhone), rawOtpCode);
        } else {
            sideEffects.afterCommit(
                    () -> smsService.sendOtp(toSmsPhone(normalizedPhone), rawOtpCode), "send password reset OTP SMS");
        }
    }

    /**
     * Completes the password reset process using a valid OTP code.
     *
     * @param phone  The registered phone number
     * @param otpCode     The 6-digit OTP received via SMS
     * @param newPassword The new plain text password
     */
    @Transactional
    public void resetPasswordWithOtp(String phone, String otpCode, String newPassword) {
        if (!passwordResetProperties.isPhoneEnabled()) {
            throw new BusinessException(ErrorCode.FEATURE_DISABLED, "Phone password reset is currently disabled");
        }

        String normalizedPhone = normalizePhone(phone);

        PasswordResetToken otpToken = tokenRepo
                .findLatestOtpForUpdate(
                        normalizedPhone, AppTime.now(), PasswordResetToken.Via.PHONE, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID, "OTP invalid or expired"));

        if (otpToken.getAttempts() >= PasswordResetAttemptRecorder.MAX_OTP_ATTEMPTS) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID, "OTP invalid or expired");
        }

        if (!otpToken.getOtpCode().equals(sha256(otpCode))) {
            if (attemptRecorder != null) {
                attemptRecorder.recordFailedOtpAttempt(otpToken.getId());
            }
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID, "OTP invalid or expired");
        }

        User user = otpToken.getUser();
        if (!isAccountActive(user)) {
            throw new BusinessException(
                    ErrorCode.ACCOUNT_INACTIVE, "Inactive or banned accounts cannot reset password");
        }
        otpToken.setUsed(true);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(AppTime.now().truncatedTo(ChronoUnit.SECONDS));
        user.setMustChangePassword(false);
        userRepo.save(user);
        tokenRepo.save(otpToken);
        refreshTokenService.revokeUserTokens(user.getId());
        accountLockoutService.unlock(user.getEmail());
        eventPublisher.publishEvent(new UserAuthChangeEvent(user.getEmail(), null));
    }

    /**
     * Normalizes Vietnamese phone number formats (e.g., converting +84 to 0).
     */
    private String normalizePhone(String phone) {
        Objects.requireNonNull(phone, "Phone number cannot be null");
        if (phone.startsWith("+84")) {
            return "0" + phone.substring(3);
        }
        return phone;
    }

    private String toSmsPhone(String normalizedPhone) {
        if (normalizedPhone != null && normalizedPhone.startsWith("0") && normalizedPhone.length() > 1) {
            return "+84" + normalizedPhone.substring(1);
        }
        return normalizedPhone;
    }

    private String sha256(String raw) {
        return com.qros.shared.security.HashUtils.sha256Hex(raw);
    }

    private String maskPhone(String phone) {
        return com.qros.shared.security.PhoneMasker.mask(phone);
    }

    private boolean isAccountActive(User user) {
        return user != null && !user.isDeleted() && user.getStatus() == UserStatus.ACTIVE;
    }

    private boolean shouldLogOtpInDev() {
        return environment.acceptsProfiles(Profiles.of("dev")) && passwordResetProperties.isDevLogOtp();
    }
}
