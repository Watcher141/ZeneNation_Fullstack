package com.zenenation.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_subscribers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailSubscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String name;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "subscribed_at", nullable = false, updatable = false)
    private LocalDateTime subscribedAt;

    @Column(name = "unsubscribed_at")
    private LocalDateTime unsubscribedAt;
}
