package com.qros.infrastructure.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * CloudinaryStorageService - Wrapper for Cloudinary API to manage cloud-based image storage.
 * Handles uploading, deleting, and replacing images for menu items, categories, and user profiles.
 */
@Slf4j
@Service
public class CloudinaryStorageService implements StorageService {

    private final Cloudinary cloudinary;

    /**
     * Initializes the Cloudinary client with credentials from application properties.
     */
    public CloudinaryStorageService(CloudinaryProperties properties) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", properties.getCloudName(),
                "api_key", properties.getApiKey(),
                "api_secret", properties.getApiSecret(),
                "secure", true));
        log.info("Cloudinary storage service initialized successfully.");
    }

    /**
     * Uploads a file from a MultipartFile request to a specific cloud folder.
     *
     * @param file The image file to upload
     * @param folder Target folder in Cloudinary
     * @return The secure URL of the uploaded image
     * @throws IOException if the upload fails
     */
    public String upload(@NonNull MultipartFile file, String folder) throws IOException {
        try {
            log.info("Initiating cloud upload to folder: {}", folder);
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary
                    .uploader()
                    .upload(file.getInputStream(), ObjectUtils.asMap("folder", folder, "resource_type", "image"));
            String url = Objects.requireNonNull(uploadResult.get("secure_url")).toString();
            log.info("Successfully uploaded image to Cloudinary. URL: {}", url);
            return url;
        } catch (Exception e) {
            log.error("Failed to upload image to Cloudinary: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Deletes an image from cloud storage using its Public ID.
     *
     * @param publicId Cloudinary public identifier
     */
    public void deleteByPublicId(String publicId) {
        if (publicId == null || publicId.isBlank() || "PENDING".equals(publicId)) return;
        try {
            log.info("Attempting to delete cloud image with PublicID: {}", publicId);
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (Exception e) {
            log.error("Failed to delete cloud image (PublicID: {}): {}", publicId, e.getMessage());
        }
    }

    /**
     * Deletes an image based on either its Public ID or its full URL.
     *
     * @param idOrUrl The cloud identifier or secure URL
     */
    public void delete(String idOrUrl) {
        if (idOrUrl == null || idOrUrl.isBlank() || "PENDING".equals(idOrUrl)) return;

        try {
            String publicId = idOrUrl.contains("/upload/") ? extractPublicId(idOrUrl) : idOrUrl;
            deleteByPublicId(publicId);
        } catch (Exception e) {
            log.error("Failed to process cloud image deletion for {}: {}", idOrUrl, e.getMessage());
        }
    }

    /**
     * Replaces an existing cloud image with a new upload.
     *
     * @param newFile The new image file
     * @param oldUrl URL of the image to be replaced
     * @param folder Target cloud folder
     * @return URL of the newly uploaded image
     * @throws IOException if the upload fails
     */
    public String replace(@NonNull MultipartFile newFile, String oldUrl, String folder) throws IOException {
        String newUrl = upload(newFile, folder);
        if (oldUrl != null) {
            delete(oldUrl);
        }
        return newUrl;
    }

    /**
     * Safely extracts the public_id from a standard Cloudinary secure URL.
     */
    private String extractPublicId(String url) {
        try {
            int uploadIndex = url.indexOf("/upload/");
            if (uploadIndex < 0) {
                return url;
            }

            String part = url.substring(uploadIndex + "/upload/".length());
            int queryIndex = part.indexOf('?');
            if (queryIndex >= 0) {
                part = part.substring(0, queryIndex);
            }

            String[] segments = part.split("/");
            int publicIdStart = 0;
            for (int index = 0; index < segments.length; index++) {
                if (segments[index].matches("v\\d+")) {
                    publicIdStart = index + 1;
                    break;
                }
            }

            String publicId = String.join("/", java.util.Arrays.copyOfRange(segments, publicIdStart, segments.length));
            int extensionIndex = publicId.lastIndexOf('.');
            if (extensionIndex > 0) {
                publicId = publicId.substring(0, extensionIndex);
            }
            return publicId;
        } catch (Exception e) {
            log.warn("Unable to extract PublicID from cloud URL: {}", url);
            return "";
        }
    }

    /**
     * Directly uploads a byte array to cloud storage. Useful for generated content like QR codes.
     *
     * @param data Byte array of the image
     * @param folder Target cloud folder
     * @param publicId Desired public identifier for the file
     * @return Map containing upload metadata
     * @throws IOException if the upload fails
     */
    public Map<String, Object> uploadBytes(byte[] data, String folder, String publicId) throws IOException {
        try {
            log.info("Uploading raw byte data to Cloudinary (PublicID: {})", publicId);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) cloudinary
                    .uploader()
                    .upload(
                            data,
                            ObjectUtils.asMap(
                                    "folder", folder,
                                    "public_id", publicId,
                                    "resource_type", "image"));
            return result;
        } catch (Exception e) {
            log.error("Failed to upload raw byte data to Cloudinary: {}", e.getMessage());
            throw e;
        }
    }
}
