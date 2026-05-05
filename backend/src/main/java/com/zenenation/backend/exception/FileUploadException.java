package com.zenenation.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when image upload to Cloudinary fails or
 * when the uploaded file is invalid.
 * Maps to HTTP 400 Bad Request.
 *
 * Usage examples:
 *   throw new FileUploadException("Only JPEG, PNG and WEBP images are allowed");
 *   throw new FileUploadException("File size exceeds the maximum limit of 10MB");
 *   throw new FileUploadException("Failed to upload image to Cloudinary");
 */
public class FileUploadException extends BaseException {

    public FileUploadException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
