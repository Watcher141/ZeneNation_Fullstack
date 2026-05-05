package com.zenenation.backend.service.impl;

import com.zenenation.backend.dto.request.AnnouncementRequest;
import com.zenenation.backend.dto.request.SubscribeRequest;
import com.zenenation.backend.dto.response.AnnouncementResponse;
import com.zenenation.backend.dto.response.PagedResponse;
import com.zenenation.backend.entity.Announcement;
import com.zenenation.backend.entity.EmailSubscriber;
import com.zenenation.backend.exception.BadRequestException;
import com.zenenation.backend.exception.ResourceNotFoundException;
import com.zenenation.backend.repository.AnnouncementRepository;
import com.zenenation.backend.repository.EmailSubscriberRepository;
import com.zenenation.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementService {

    private final AnnouncementRepository announcementRepo;
    private final EmailSubscriberRepository subscriberRepo;
    private final EmailService emailService;

    // ── Public — get active announcements for website banner ──────────────────

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getActiveAnnouncements() {
        return announcementRepo.findCurrentlyActive(LocalDateTime.now())
                .stream().map(this::toResponse).toList();
    }

    // ── Subscribe ─────────────────────────────────────────────────────────────

    @Transactional
    public String subscribe(SubscribeRequest request) {
        if (subscriberRepo.existsByEmail(request.getEmail())) {
            // Re-activate if previously unsubscribed
            EmailSubscriber existing = subscriberRepo.findByEmail(request.getEmail()).get();
            if (!existing.getIsActive()) {
                existing.setIsActive(true);
                existing.setUnsubscribedAt(null);
                subscriberRepo.save(existing);
                emailService.sendSubscriptionWelcomeEmail(request.getEmail(), request.getName());
                return "Welcome back! You've been re-subscribed.";
            }
            return "You're already subscribed!";
        }

        EmailSubscriber subscriber = EmailSubscriber.builder()
                .email(request.getEmail())
                .name(request.getName())
                .isActive(true)
                .build();
        subscriberRepo.save(subscriber);

        emailService.sendSubscriptionWelcomeEmail(request.getEmail(), request.getName());
        log.info("New subscriber: {}", request.getEmail());
        return "Successfully subscribed! Check your email for a welcome message.";
    }

    @Transactional
    public String unsubscribe(String email) {
        EmailSubscriber subscriber = subscriberRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found"));
        subscriber.setIsActive(false);
        subscriber.setUnsubscribedAt(LocalDateTime.now());
        subscriberRepo.save(subscriber);
        return "Successfully unsubscribed.";
    }

    // ── Admin — Announcements CRUD ────────────────────────────────────────────

    @Transactional
    public AnnouncementResponse createAnnouncement(AnnouncementRequest request) {
        Announcement announcement = Announcement.builder()
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType() != null ? request.getType() : Announcement.AnnouncementType.INFO)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .emailSent(false)
                .build();

        announcement = announcementRepo.save(announcement);

        // Send email blast if requested
        if (Boolean.TRUE.equals(request.getSendEmailBlast())) {
            sendEmailBlast(announcement);
        }

        return toResponse(announcement);
    }

    @Transactional
    public AnnouncementResponse updateAnnouncement(Long id, AnnouncementRequest request) {
        Announcement announcement = announcementRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        announcement.setTitle(request.getTitle());
        announcement.setMessage(request.getMessage());
        if (request.getType() != null) announcement.setType(request.getType());
        if (request.getIsActive() != null) announcement.setIsActive(request.getIsActive());
        announcement.setStartsAt(request.getStartsAt());
        announcement.setEndsAt(request.getEndsAt());

        // Send email blast if requested and not already sent
        if (Boolean.TRUE.equals(request.getSendEmailBlast()) && !announcement.getEmailSent()) {
            sendEmailBlast(announcement);
        }

        return toResponse(announcementRepo.save(announcement));
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        Announcement announcement = announcementRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));
        announcementRepo.delete(announcement);
    }

    @Transactional
    public AnnouncementResponse toggleAnnouncement(Long id) {
        Announcement announcement = announcementRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));
        announcement.setIsActive(!announcement.getIsActive());
        return toResponse(announcementRepo.save(announcement));
    }

    @Transactional(readOnly = true)
    public PagedResponse<AnnouncementResponse> getAllAnnouncements(int page, int size) {
        var page_ = announcementRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return PagedResponse.of(page_.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public long getSubscriberCount() {
        return subscriberRepo.countByIsActiveTrue();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void sendEmailBlast(Announcement announcement) {
        List<String> emails = subscriberRepo.findByIsActiveTrue()
                .stream().map(EmailSubscriber::getEmail).toList();

        if (emails.isEmpty()) {
            log.info("No subscribers to send announcement to");
            return;
        }

        emailService.sendAnnouncementEmail(
                emails,
                announcement.getTitle(),
                announcement.getMessage(),
                announcement.getType().name()
        );

        announcement.setEmailSent(true);
        announcementRepo.save(announcement);
        log.info("Announcement email blast sent to {} subscribers", emails.size());
    }

    private AnnouncementResponse toResponse(Announcement a) {
        return AnnouncementResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .message(a.getMessage())
                .type(a.getType().name())
                .isActive(a.getIsActive())
                .emailSent(a.getEmailSent())
                .isCurrentlyActive(a.isCurrentlyActive())
                .startsAt(a.getStartsAt())
                .endsAt(a.getEndsAt())
                .createdAt(a.getCreatedAt())
                .build();
    }
}