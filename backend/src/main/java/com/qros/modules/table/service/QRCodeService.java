package com.qros.modules.table.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QRCodeService {

    private static final String PNG_FORMAT = "PNG";

    private static final int DEFAULT_MARGIN = 1;
    private static final String DEFAULT_CHARACTER_SET = "UTF-8";
    private static final ErrorCorrectionLevel DEFAULT_ERROR_CORRECTION = ErrorCorrectionLevel.H;

    public byte[] generateQRCodeImage(@NonNull String text, int width, int height) {
        validateInput(text, width, height);

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, buildHints());

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, PNG_FORMAT, outputStream);

            return outputStream.toByteArray();
        } catch (WriterException e) {
            log.error("ZXing failed to encode QR payload, payloadLength={}: {}", text.length(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.TABLE_QR_GENERATION_FAILED, "Unable to encode QR code payload", e);
        } catch (Exception e) {
            log.error("Failed to generate QR image, payloadLength={}: {}", text.length(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.TABLE_QR_GENERATION_FAILED, "Unable to generate QR code image", e);
        }
    }

    private Map<EncodeHintType, Object> buildHints() {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, DEFAULT_ERROR_CORRECTION);
        hints.put(EncodeHintType.MARGIN, DEFAULT_MARGIN);
        hints.put(EncodeHintType.CHARACTER_SET, DEFAULT_CHARACTER_SET);
        return hints;
    }

    private void validateInput(String text, int width, int height) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.TABLE_QR_GENERATION_FAILED, "QR code payload cannot be empty");
        }

        if (width <= 0 || height <= 0) {
            throw new BusinessException(ErrorCode.TABLE_QR_GENERATION_FAILED, "QR code dimensions must be positive");
        }
    }
}
