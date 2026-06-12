package com.qros.modules.table.service;

import com.qros.infrastructure.storage.StorageService;
import com.qros.modules.table.dto.internal.TableQrMedia;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableQrService {

    private static final int QR_WIDTH = 300;
    private static final int QR_HEIGHT = 300;
    private static final String TABLE_QR_FOLDER = "order_by_qr/tables";
    private static final String TABLE_QR_PUBLIC_ID_PREFIX = "qr_";

    private final QRCodeService qrCodeService;
    private final StorageService storageService;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public TableQrMedia generate(@NonNull String tableCode) {
        try {
            String qrContent = buildOrderingUrl(tableCode);
            byte[] qrBytes = qrCodeService.generateQRCodeImage(qrContent, QR_WIDTH, QR_HEIGHT);

            String publicId = TABLE_QR_PUBLIC_ID_PREFIX + tableCode;
            Map<String, Object> result = storageService.uploadBytes(
                    qrBytes,
                    TABLE_QR_FOLDER,
                    publicId
            );

            return new TableQrMedia(
                    result.get("secure_url").toString(),
                    result.get("public_id").toString()
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate/upload table QR for tableCode {}: {}", tableCode, e.getMessage(), e);
            throw new BusinessException(
                    ErrorCode.TABLE_QR_GENERATION_FAILED,
                    "System error generating table QR code",
                    e
            );
        }
    }

    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank() || "PENDING".equals(publicId)) {
            return;
        }

        storageService.delete(publicId);
    }

    private String buildOrderingUrl(String tableCode) {
        return UriComponentsBuilder
                .fromHttpUrl(frontendBaseUrl)
                .path("/menu")
                .queryParam("tableCode", tableCode)
                .toUriString();
    }
}