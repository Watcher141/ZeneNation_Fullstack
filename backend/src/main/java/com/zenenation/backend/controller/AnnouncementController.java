package com.zenenation.backend.controller;

import com.zenenation.backend.dto.request.AnnouncementRequest;
import com.zenenation.backend.dto.request.SubscribeRequest;
import com.zenenation.backend.dto.response.AnnouncementResponse;
import com.zenenation.backend.dto.response.ApiResponse;
import com.zenenation.backend.dto.response.PagedResponse;
import com.zenenation.backend.service.impl.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    // ── Public ────────────────────────────────────────────────────────────────

    /** GET /api/v1/announcements/active — shown on website banner */
    @GetMapping("/announcements/active")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> getActiveAnnouncements() {
        return ResponseEntity.ok(ApiResponse.success("Announcements fetched",
                announcementService.getActiveAnnouncements()));
    }

    /** POST /api/v1/subscribers/subscribe */
    @PostMapping("/subscribers/subscribe")
    public ResponseEntity<ApiResponse<String>> subscribe(
            @Valid @RequestBody SubscribeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                announcementService.subscribe(request), null));
    }

    /** POST /api/v1/subscribers/unsubscribe */
    @PostMapping("/subscribers/unsubscribe")
    public ResponseEntity<ApiResponse<String>> unsubscribe(
            @RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success(
                announcementService.unsubscribe(email), null));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    /** GET /api/v1/admin/announcements */
    @GetMapping("/admin/announcements")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<AnnouncementResponse>>> getAllAnnouncements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Announcements fetched",
                announcementService.getAllAnnouncements(page, size)));
    }

    /** POST /api/v1/admin/announcements */
    @PostMapping("/admin/announcements")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createAnnouncement(
            @Valid @RequestBody AnnouncementRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Announcement created",
                announcementService.createAnnouncement(request)));
    }

    /** PUT /api/v1/admin/announcements/{id} */
    @PutMapping("/admin/announcements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Announcement updated",
                announcementService.updateAnnouncement(id, request)));
    }

    /** DELETE /api/v1/admin/announcements/{id} */
    @DeleteMapping("/admin/announcements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success("Announcement deleted"));
    }

    /** PATCH /api/v1/admin/announcements/{id}/toggle */
    @PatchMapping("/admin/announcements/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> toggleAnnouncement(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Toggled",
                announcementService.toggleAnnouncement(id)));
    }

    /** GET /api/v1/admin/subscribers/count */
    @GetMapping("/admin/subscribers/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getSubscriberCount() {
        return ResponseEntity.ok(ApiResponse.success("Subscriber count",
                announcementService.getSubscriberCount()));
    }
}