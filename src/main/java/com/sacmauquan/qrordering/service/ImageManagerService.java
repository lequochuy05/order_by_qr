package com.sacmauquan.qrordering.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class ImageManagerService {

    private final Cloudinary cloudinary;

    // 🔹 Inject giá trị từ application.properties
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
    }

    /** Upload file mới */
    public String upload(MultipartFile file, String folder) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "auto"
        ));
        return uploadResult.get("secure_url").toString();
    }

    /** Xóa ảnh khỏi Cloudinary */
    public void delete(String url) {
        try {
            if (url == null || !url.contains("/upload/")) return;
            String publicId = extractPublicId(url);
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (Exception e) {
            System.err.println("❌ Không thể xóa ảnh Cloudinary: " + e.getMessage());
        }
    }

    /** Thay ảnh cũ bằng ảnh mới */
    public String replace(MultipartFile newFile, String oldUrl, String folder) throws IOException {
        delete(oldUrl);
        return upload(newFile, folder);
    }

    /** Tách public_id từ URL */
    private String extractPublicId(String url) {
        String publicId = url.substring(url.indexOf("/upload/") + 8);
        publicId = publicId.substring(publicId.indexOf("/") + 1); // bỏ version (v...)
        if (publicId.contains(".")) publicId = publicId.substring(0, publicId.lastIndexOf(".")); // bỏ đuôi file
        return publicId;
    }

    /** Upload dữ liệu QR code (byte[]) lên Cloudinary */
    public Map uploadBytes(byte[] data, String folder, String publicId) throws IOException {
        return cloudinary.uploader().upload(data, ObjectUtils.asMap(
                "folder", folder,
                "public_id", publicId,
                "resource_type", "image"
        ));
    }
}
