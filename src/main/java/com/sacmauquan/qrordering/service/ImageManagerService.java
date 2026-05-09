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
 * ImageManagerService - Quản lý tải lên và xóa hình ảnh trên Cloudinary.
 */
@Slf4j
@Service
public class ImageManagerService {

    private final Cloudinary cloudinary;

    public ImageManagerService(
            @Value("${cloudinary.cloud_name}") String cloudName,
            @Value("${cloudinary.api_key}") String apiKey,
            @Value("${cloudinary.api_secret}") String apiSecret
    ) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
        log.info("Cloudinary Service initialized successfully.");
    }

    /**
     * Upload file từ MultipartFile.
     */
    public String upload(@NonNull MultipartFile file, String folder) throws IOException {
        try {
            log.info("Đang upload file lên thư mục: {}", folder);
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "auto"
            ));
            String url = Objects.requireNonNull(uploadResult.get("secure_url")).toString();
            log.info("Upload thành công. URL: {}", url);
            return url;
        } catch (Exception e) {
            log.error("Lỗi khi upload file lên Cloudinary: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Xóa ảnh dựa trên Public ID.
     */
    public void deleteByPublicId(String publicId) {
        if (publicId == null || publicId.isBlank() || "PENDING".equals(publicId)) return;
        try {
            log.info("Đang xóa ảnh trên Cloudinary với PublicID: {}", publicId);
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (Exception e) {
            log.error("Không thể xóa ảnh Cloudinary (PublicID: {}): {}", publicId, e.getMessage());
        }
    }

    /**
     * Xóa ảnh dựa trên URL hoặc Public ID.
     */
    public void delete(String idOrUrl) {
        if (idOrUrl == null || idOrUrl.isBlank() || "PENDING".equals(idOrUrl))
            return;

        try {
            String publicId = idOrUrl.contains("/upload/") ? extractPublicId(idOrUrl) : idOrUrl;
            deleteByPublicId(publicId);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý xóa ảnh {}: {}", idOrUrl, e.getMessage());
        }
    }

    /**
     * Thay thế ảnh cũ bằng ảnh mới.
     */
    public String replace(@NonNull MultipartFile newFile, String oldUrl, String folder) throws IOException {
        if (oldUrl != null) {
            delete(oldUrl);
        }
        return upload(newFile, folder);
    }

    /**
     * Tách public_id từ URL Cloudinary một cách an toàn hơn.
     */
    private String extractPublicId(String url) {
        // Ví dụ URL: https://res.cloudinary.com/demo/image/upload/v1234567/sample.jpg
        // PublicID sẽ là: sample
        try {
            String part = url.substring(url.indexOf("/upload/") + 8);
            // Bỏ qua phần version (v1234567/) nếu có
            if (part.contains("/")) {
                part = part.substring(part.indexOf("/") + 1);
            }
            // Bỏ đuôi file (.jpg, .png...)
            if (part.contains(".")) {
                part = part.substring(0, part.lastIndexOf("."));
            }
            return part;
        } catch (Exception e) {
            log.warn("Không thể tách PublicID từ URL: {}", url);
            return "";
        }
    }

    /**
     * Upload dữ liệu bytes (Dùng cho QR Code).
     */
    public Map<String, Object> uploadBytes(byte[] data, String folder, String publicId) throws IOException {
        try {
            log.info("Đang upload bytes lên Cloudinary (PublicID: {})", publicId);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) cloudinary.uploader().upload(data, ObjectUtils.asMap(
                    "folder", folder,
                    "public_id", publicId,
                    "resource_type", "image"
            ));
            return result;
        } catch (Exception e) {
            log.error("Lỗi khi upload bytes lên Cloudinary: {}", e.getMessage());
            throw e;
        }
    }
}
