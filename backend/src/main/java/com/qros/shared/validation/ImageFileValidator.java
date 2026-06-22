package com.qros.shared.validation;

import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageFileValidator {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    public void validate(@NonNull MultipartFile file) {
        if (file.isEmpty()) {
            throw invalid("Image file cannot be empty");
        }

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw invalid("Image file cannot exceed 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw invalid("Only JPEG, PNG, and WebP images are allowed");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw invalid("Image file extension must be jpg, jpeg, png, or webp");
        }

        validateMagicBytes(file, contentType.toLowerCase(Locale.ROOT));
    }

    private void validateMagicBytes(MultipartFile file, String contentType) {
        byte[] header = new byte[12];
        int read;
        try (InputStream inputStream = file.getInputStream()) {
            read = inputStream.read(header);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_INVALID, "Unable to inspect image file", exception);
        }

        boolean valid =
                switch (contentType) {
                    case "image/jpeg" -> read >= 3
                            && (header[0] & 0xFF) == 0xFF
                            && (header[1] & 0xFF) == 0xD8
                            && (header[2] & 0xFF) == 0xFF;
                    case "image/png" -> read >= 8
                            && (header[0] & 0xFF) == 0x89
                            && header[1] == 0x50
                            && header[2] == 0x4E
                            && header[3] == 0x47
                            && header[4] == 0x0D
                            && header[5] == 0x0A
                            && header[6] == 0x1A
                            && header[7] == 0x0A;
                    case "image/webp" -> read >= 12
                            && header[0] == 0x52
                            && header[1] == 0x49
                            && header[2] == 0x46
                            && header[3] == 0x46
                            && header[8] == 0x57
                            && header[9] == 0x45
                            && header[10] == 0x42
                            && header[11] == 0x50;
                    default -> false;
                };

        if (!valid) {
            throw invalid("File content does not match the declared image type");
        }
    }

    private String extensionOf(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }

        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return null;
        }

        return filename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    private BusinessException invalid(String message) {
        return new BusinessException(
                ErrorCode.FILE_INVALID,
                message,
                Map.of(
                        "allowedContentTypes", ALLOWED_CONTENT_TYPES,
                        "maxBytes", MAX_IMAGE_SIZE_BYTES));
    }
}
