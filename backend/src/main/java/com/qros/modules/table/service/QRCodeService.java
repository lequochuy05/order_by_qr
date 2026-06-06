package com.qros.modules.table.service;

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
 * QRCodeService - Generates high-quality, professional QR codes using the ZXing library.
 * Supports Base64 encoding for direct frontend rendering and byte-array generation for cloud storage.
 */
@Slf4j
@Service
public class QRCodeService {

    /**
     * Generates a QR code and returns it as a Base64-encoded Data URL string.
     * Results are cached to optimize performance for frequently requested table codes.
     * 
     * @param text The payload content for the QR code
     * @param width Image width in pixels
     * @param height Image height in pixels
     * @return Base64 data URL (e.g., "data:image/png;base64,...")
     */
    @Cacheable(value = "qrcodes", key = "#text + #width + #height")
    public String generateQRCodeBase64(String text, int width, int height) {
        try {
            byte[] imageBytes = generateQRCodeImage(text, width, height);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            log.error("Failed to generate Base64-encoded QR code: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Generates a raw PNG byte array for a QR code with advanced error correction.
     * 
     * @param text The payload content
     * @param width Image width
     * @param height Image height
     * @return Byte array containing the PNG image data
     * @throws IOException if encoding or stream writing fails
     */
    public byte[] generateQRCodeImage(String text, int width, int height) throws IOException {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            // Configure advanced encoding hints for better readability and resilience
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // High error correction level
            hints.put(EncodeHintType.MARGIN, 1); // Narrow margin for better space utilization
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();

        } catch (WriterException e) {
            log.error("ZXing encoding error for payload [{}]: {}", text, e.getMessage());
            throw new IOException("Unable to generate QR code using ZXing", e);
        }
    }
}
