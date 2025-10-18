package com.sacmauquan.qrordering.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class ImageManagerService {

    private final Cloudinary cloudinary;

    public ImageManagerService() {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dr0rjwfoc",      
                "api_key", "967974352521679",
                "api_secret", "Uw9z6nWLGjwQ5YGnTzfSOKWN-tA",
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

    /** Xóa ảnh khỏi Cloudinary (nếu có) */
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
}
