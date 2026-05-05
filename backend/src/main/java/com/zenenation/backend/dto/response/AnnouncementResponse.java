package com.zenenation.backend.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AnnouncementResponse {
    private Long id;
    private String title;
    private String message;
    private String type;
    private Boolean isActive;
    private Boolean emailSent;
    private Boolean isCurrentlyActive;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private LocalDateTime createdAt;
}