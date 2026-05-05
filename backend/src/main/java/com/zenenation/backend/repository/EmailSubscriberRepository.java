package com.zenenation.backend.repository;

import com.zenenation.backend.entity.EmailSubscriber;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailSubscriberRepository extends JpaRepository<EmailSubscriber, Long> {
    Optional<EmailSubscriber> findByEmail(String email);
    boolean existsByEmail(String email);
    List<EmailSubscriber> findByIsActiveTrue();
    Page<EmailSubscriber> findAllByOrderBySubscribedAtDesc(Pageable pageable);
    long countByIsActiveTrue();
}
