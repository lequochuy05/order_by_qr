package com.sacmauquan.qrordering.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * QRCodeService - Tạo mã QR chuyên nghiệp cho bàn ăn và thanh toán.
 */
@Slf4j
@Service
public class QRCodeService {

    /**
     * Tạo mã QR dưới dạng chuỗi Base64 
     */
    @Cacheable(value = "qrcodes", key = "#text + #width + #height")
    public String generateQRCodeBase64(String text, int width, int height) {
        try {
            byte[] imageBytes = generateQRCodeImage(text, width, height);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            log.error("Lỗi khi tạo Base64 QR: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Tạo mã QR dưới dạng byte array với cấu hình nâng cao
     */
    public byte[] generateQRCodeImage(String text, int width, int height) throws IOException {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            // Cấu hình nâng cao
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();

        } catch (WriterException e) {
            log.error("Lỗi thư viện ZXing khi tạo QR cho [{}]: {}", text, e.getMessage());
            throw new IOException("Không thể tạo mã QR", e);
        }
    }
}
