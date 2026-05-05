package com.zenenation.backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Contract for all Cloudinary image operations.
 *
 * Keeping this as an interface means:
 * - Easy to mock in unit tests (no real Cloudinary calls needed)
 * - Easy to swap to a different provider later (S3, Supabase Storage etc.)
 *   without touching any service that uses images
 */
public interface CloudinaryService {

    /**
     * Upload an image file to Cloudinary.
     *
     * @param file    the image file from the multipart request
     * @param folder  Cloudinary folder to store in (e.g. "ecommerce/products")
     * @return        Map with "url" and "publicId" keys
     *                url      → CDN URL to display the image
     *                publicId → Cloudinary ID needed to delete later
     */
    Map<String, String> uploadImage(MultipartFile file, String folder);

    /**
     * Delete an image from Cloudinary by its public ID.
     * Called when admin removes a product image or replaces a category image.
     *
     * @param publicId  the Cloudinary public ID stored during upload
     */
    void deleteImage(String publicId);

    /**
     * Replace an existing image — delete old one and upload new one atomically.
     * Prevents orphaned images on Cloudinary if upload succeeds but delete fails.
     *
     * @param oldPublicId  public ID of the image to delete
     * @param newFile      new image file to upload
     * @param folder       Cloudinary folder for the new image
     * @return             Map with "url" and "publicId" for the new image
     */
    Map<String, String> replaceImage(String oldPublicId, MultipartFile newFile, String folder);

    /**
     * Validate that a file is a valid image before uploading.
     * Checks: not null, not empty, correct MIME type, within size limit.
     *
     * @param file  the file to validate
     * @throws      FileUploadException if validation fails
     */
    void validateImageFile(MultipartFile file);
}
