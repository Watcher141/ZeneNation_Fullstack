package com.zenenation.backend.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.zenenation.backend.exception.FileUploadException;
import com.zenenation.backend.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles all image operations with Cloudinary:
 * upload, delete, replace, validate.
 *
 * IMPORTANT DESIGN DECISIONS:
 *
 * 1. We always store the Cloudinary publicId when uploading.
 *    Without it, we can display images but never delete them.
 *    That would mean wasted storage and increasing Cloudinary bills.
 *
 * 2. File validation happens BEFORE any Cloudinary API call.
 *    Fail fast — don't waste Cloudinary API quota on invalid files.
 *
 * 3. replaceImage() deletes old BEFORE uploading new.
 *    If upload fails → old image is gone but nothing is orphaned.
 *    Alternative: upload new first, then delete old. Either is acceptable.
 *    We chose delete-first to avoid having two images temporarily.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    // Allowed image MIME types
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    // Max file size: 10MB in bytes (matches application.yml multipart config)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    // -------------------------------------------------------------------------
    // UPLOAD
    // -------------------------------------------------------------------------

    @Override
    public Map<String, String> uploadImage(MultipartFile file, String folder) {
        // Validate before hitting Cloudinary API
        validateImageFile(file);

        try {
            // Generate a unique public ID to avoid collisions
            // Format: folder/uuid — e.g. "ecommerce/products/a1b2c3d4..."
            String publicId = folder + "/" + UUID.randomUUID();

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id",     publicId,
                            "folder",        folder,
                            "resource_type", "image",
                            // Auto-quality reduction without visible quality loss
                            "quality",       "auto",
                            // Auto-select best format (WebP for browsers that support it)
                            "fetch_format",  "auto",
                            // Overwrite if same public_id exists (safety net)
                            "overwrite",     true
                    )
            );

            String url = (String) uploadResult.get("secure_url");
            String uploadedPublicId = (String) uploadResult.get("public_id");

            log.info("Image uploaded to Cloudinary: publicId={}", uploadedPublicId);

            return Map.of(
                    "url",      url,
                    "publicId", uploadedPublicId
            );

        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary: {}", e.getMessage());
            throw new FileUploadException("Failed to upload image. Please try again.");
        }
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Override
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            log.warn("deleteImage called with null/blank publicId — skipping");
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", "image")
            );

            String resultStatus = (String) result.get("result");

            if ("ok".equals(resultStatus)) {
                log.info("Image deleted from Cloudinary: publicId={}", publicId);
            } else {
                // "not found" means image was already deleted — log but don't throw
                log.warn("Cloudinary delete result for publicId={}: {}", publicId, resultStatus);
            }

        } catch (IOException e) {
            // Log the error but don't throw — a failed delete should not
            // break the user's operation (product update, category update etc.)
            // The orphaned image can be cleaned up manually on Cloudinary dashboard.
            log.error("Failed to delete image from Cloudinary: publicId={}, error={}",
                    publicId, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // REPLACE
    // -------------------------------------------------------------------------

    @Override
    public Map<String, String> replaceImage(String oldPublicId, MultipartFile newFile, String folder) {
        // Validate new file first — if it's invalid, don't delete the old one
        validateImageFile(newFile);

        // Delete old image
        if (oldPublicId != null && !oldPublicId.isBlank()) {
            deleteImage(oldPublicId);
        }

        // Upload new image
        return uploadImage(newFile, folder);
    }

    // -------------------------------------------------------------------------
    // VALIDATE
    // -------------------------------------------------------------------------

    @Override
    public void validateImageFile(MultipartFile file) {
        // Check file is present
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("Image file is required and cannot be empty");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileUploadException(
                    String.format("File size %.2f MB exceeds the maximum allowed size of 10MB",
                            file.getSize() / (1024.0 * 1024.0))
            );
        }

        // Check MIME type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new FileUploadException(
                    "Invalid file type. Only JPEG, PNG, and WebP images are allowed. " +
                    "Received: " + (contentType != null ? contentType : "unknown")
            );
        }
    }
}
