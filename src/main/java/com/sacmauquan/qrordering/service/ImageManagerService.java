package com.sacmauquan.qrordering.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * ImageManagerService - Manages uploading and deleting images on Cloudinary.
 */
@Slf4j
@Service
public class ImageManagerService {

    private final Cloudinary cloudinary;

    /**
     * Initializes the Cloudinary service with the provided configuration.
     */
    public ImageManagerService(
            @Value("${cloudinary.cloud_name}") String cloudName,
            @Value("${cloudinary.api_key}") String apiKey,
            @Value("${cloudinary.api_secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
        log.info("Cloudinary Service initialized successfully.");
    }

    /**
     * Upload file từ MultipartFile.
     */
    public String upload(@NonNull MultipartFile file, String folder) throws IOException {
        try {
            log.info("Uploading file to folder: {}", folder);
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "auto"));
            String url = Objects.requireNonNull(uploadResult.get("secure_url")).toString();
            log.info("Upload successfully. URL: {}", url);
            return url;
        } catch (Exception e) {
            log.error("Failed to upload file to Cloudinary: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Delete image by Public ID.
     */
    public void deleteByPublicId(String publicId) {
        if (publicId == null || publicId.isBlank() || "PENDING".equals(publicId))
            return;
        try {
            log.info("Deleting image on Cloudinary with PublicID: {}", publicId);
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (Exception e) {
            log.error("Failed to delete image on Cloudinary (PublicID: {}): {}", publicId, e.getMessage());
        }
    }

    /**
     * Delete image by ID or Public ID.
     */
    public void delete(String idOrUrl) {
        if (idOrUrl == null || idOrUrl.isBlank() || "PENDING".equals(idOrUrl))
            return;

        try {
            String publicId = idOrUrl.contains("/upload/") ? extractPublicId(idOrUrl) : idOrUrl;
            deleteByPublicId(publicId);
        } catch (Exception e) {
            log.error("Failed to process delete image {}: {}", idOrUrl, e.getMessage());
        }
    }

    /**
     * Replace old image with new image.
     */
    public String replace(@NonNull MultipartFile newFile, String oldUrl, String folder) throws IOException {
        if (oldUrl != null) {
            delete(oldUrl);
        }
        return upload(newFile, folder);
    }

    /**
     * Extract public_id from Cloudinary URL more safely.
     */
    private String extractPublicId(String url) {
        // URL example: https://res.cloudinary.com/demo/image/upload/v1234567/sample.jpg
        // PublicID will be: sample
        try {
            String part = url.substring(url.indexOf("/upload/") + 8);
            // Skip version (v1234567/) if exists
            if (part.contains("/")) {
                part = part.substring(part.indexOf("/") + 1);
            }
            // Remove file extension (.jpg, .png, .webp...)
            if (part.contains(".")) {
                part = part.substring(0, part.lastIndexOf("."));
            }
            return part;
        } catch (Exception e) {
            log.warn("Failed to extract PublicID from URL: {}", url);
            return "";
        }
    }

    /**
     * Upload data bytes (For QR Code).
     */
    public Map<String, Object> uploadBytes(byte[] data, String folder, String publicId) throws IOException {
        try {
            log.info("Uploading bytes to Cloudinary (PublicID: {})", publicId);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) cloudinary.uploader().upload(data, ObjectUtils.asMap(
                    "folder", folder,
                    "public_id", publicId,
                    "resource_type", "image"));
            return result;
        } catch (Exception e) {
            log.error("Failed to upload bytes to Cloudinary: {}", e.getMessage());
            throw e;
        }
    }
}
