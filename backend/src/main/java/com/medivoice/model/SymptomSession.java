package com.medivoice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "symptom_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SymptomSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "session_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "triage_level")
    @Enumerated(EnumType.STRING)
    private EmergencyLevel triageLevel = EmergencyLevel.LOW;

    @Column(name = "triage_reason")
    private String triageReason;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "remote_address")
    private String remoteAddress;

    public enum SessionStatus {
        ACTIVE,
        CLOSED,
        ERROR
    }
}
