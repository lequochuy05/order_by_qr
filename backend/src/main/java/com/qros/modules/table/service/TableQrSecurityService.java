package com.qros.modules.table.service;

import com.qros.core.config.AppProperties;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.security.JwtProperties;
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
    private static final String DEFAULT_FALLBACK_SECRET = "qros-table-qr-cryptographic-secret-key-2026";

    private final AppProperties appProperties;
    private final JwtProperties jwtProperties;

    public TableQrSecurityService(AppProperties appProperties, JwtProperties jwtProperties) {
        this.appProperties = appProperties;
        this.jwtProperties = jwtProperties;
    }

    public String generateSignature(@NonNull String tableCode) {
        try {
            byte[] secretBytes = resolveSecret().getBytes(StandardCharsets.UTF_8);
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

    private String resolveSecret() {
        if (appProperties != null && appProperties.getSecurity() != null) {
            String qrSecret = appProperties.getSecurity().getQrSecret();
            if (StringUtils.hasText(qrSecret)) {
                return qrSecret.trim();
            }
        }
        if (jwtProperties != null && StringUtils.hasText(jwtProperties.getSecret())) {
            return jwtProperties.getSecret().trim();
        }
        return DEFAULT_FALLBACK_SECRET;
    }
}
