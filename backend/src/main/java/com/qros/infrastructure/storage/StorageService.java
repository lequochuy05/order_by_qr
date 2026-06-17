package com.qros.infrastructure.storage;

import java.io.IOException;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interface for abstract storage operations.
 */
public interface StorageService {
    /**
     * Uploads a file to cloud storage.
     *
     * @param file   The image file to upload
     * @param folder Target folder in Cloudinary
     * @return The secure URL of the uploaded image
     * @throws IOException if the upload fails
     */
    String upload(MultipartFile file, String folder) throws IOException;

    /**
     * Deletes an image from cloud storage.
     *
     * @param idOrUrl The cloud identifier or secure URL
     */
    void delete(String idOrUrl);

    /**
     * Replaces an existing cloud image with a new upload.
     *
     * @param newFile The new image file
     * @param oldUrl  URL of the image to be replaced
     * @param folder  Target cloud folder
     * @return URL of the newly uploaded image
     * @throws IOException if the upload fails
     */
    String replace(MultipartFile newFile, String oldUrl, String folder) throws IOException;

    /**
     * Uploads raw byte data directly to cloud storage.
     *
     * @param data     Byte array of the image
     * @param folder   Target cloud folder
     * @param publicId Desired public identifier for the file
     * @return Map containing upload metadata
     * @throws IOException if the upload fails
     */
    Map<String, Object> uploadBytes(byte[] data, String folder, String publicId) throws IOException;
}
