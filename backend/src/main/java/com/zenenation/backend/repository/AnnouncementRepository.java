package com.zenenation.backend.repository;

import com.zenenation.backend.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /** Active announcements visible on the website right now */
    @Query("""
        SELECT a FROM Announcement a
        WHERE a.isActive = true
        AND (a.startsAt IS NULL OR a.startsAt <= :now)
        AND (a.endsAt   IS NULL OR a.endsAt   >= :now)
        ORDER BY a.createdAt DESC
    """)
    List<Announcement> findCurrentlyActive(LocalDateTime now);

    Page<Announcement> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
