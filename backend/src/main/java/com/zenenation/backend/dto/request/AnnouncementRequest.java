package com.zenenation.backend.dto.request;

import com.zenenation.backend.entity.Announcement.AnnouncementType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnouncementRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    private AnnouncementType type = AnnouncementType.INFO;

    private Boolean isActive = true;

    private LocalDateTime startsAt;
    private LocalDateTime endsAt;

    /** If true, send an email blast to all active subscribers immediately */
    private Boolean sendEmailBlast = false;
}