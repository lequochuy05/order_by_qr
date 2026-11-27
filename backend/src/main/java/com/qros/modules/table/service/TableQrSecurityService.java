package com.qros.modules.table.service;

import com.qros.shared.config.AppProperties;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TableQrSecurityService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String secret;

    public TableQrSecurityService(AppProperties appProperties) {
        String configuredSecret = null;
        if (appProperties != null) {
            if (appProperties.getQr() != null
                    && StringUtils.hasText(appProperties.getQr().getSecret())) {
                configuredSecret = appProperties.getQr().getSecret();
            } else if (appProperties.getSecurity() != null
                    && StringUtils.hasText(appProperties.getSecurity().getQrSecret())) {
                configuredSecret = appProperties.getSecurity().getQrSecret();
            }
        }
        if (!StringUtils.hasText(configuredSecret)) {
            throw new IllegalStateException(
                    "app.security.qr-secret (or app.qr.secret) must be configured using QR_SIGNING_SECRET");
        }
        this.secret = configuredSecret.trim();
    }

    public String generateSignature(@NonNull String tableCode) {
        try {
            byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGORITHM));
            byte[] hmacBytes = mac.doFinal(tableCode.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (Exception e) {
            throw new BusinessException(
                    ErrorCode.TABLE_QR_GENERATION_FAILED, "Failed to generate cryptographic QR signature", e);
        }
    }

    public void validateSignature(@NonNull String tableCode, String signature) {
        if (!StringUtils.hasText(signature)) {
            throw new BusinessException(ErrorCode.TABLE_QR_SIGNATURE_INVALID, "Table QR signature is required");
        }

        String expectedSignature = generateSignature(tableCode);
        byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = signature.trim().getBytes(StandardCharsets.UTF_8);

        if (!MessageDigest.isEqual(expectedBytes, providedBytes)) {
            throw new BusinessException(ErrorCode.TABLE_QR_SIGNATURE_INVALID, "Invalid or expired table QR signature");
        }
    }

    public boolean isSignatureValid(@NonNull String tableCode, String signature) {
        if (!StringUtils.hasText(signature)) {
            return false;
        }
        try {
            String expectedSignature = generateSignature(tableCode);
            byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
            byte[] providedBytes = signature.trim().getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(expectedBytes, providedBytes);
        } catch (Exception e) {
            return false;
        }
    }
}
