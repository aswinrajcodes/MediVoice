package com.medivoice.service;

import com.medivoice.model.EmergencyLevel;
import com.medivoice.model.SymptomSession;
import com.medivoice.repository.SessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class SessionService {

    private final SessionRepository sessionRepository;
    private final TranscriptArchiveService transcriptArchiveService;

    public SessionService(SessionRepository sessionRepository,
                          TranscriptArchiveService transcriptArchiveService) {
        this.sessionRepository = sessionRepository;
        this.transcriptArchiveService = transcriptArchiveService;
    }

    @Transactional
    public SymptomSession createSession(String userAgent, String remoteAddress) {
        SymptomSession session = new SymptomSession();
        session.setStatus(SymptomSession.SessionStatus.ACTIVE);
        session.setCreatedAt(LocalDateTime.now());
        session.setUserAgent(userAgent);
        session.setRemoteAddress(remoteAddress);
        session.setTriageLevel(EmergencyLevel.LOW);

        SymptomSession saved = sessionRepository.save(session);
        log.info("Created session: {}", saved.getId());
        return saved;
    }

    @Transactional
    public void closeSession(String sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setStatus(SymptomSession.SessionStatus.CLOSED);
            session.setClosedAt(LocalDateTime.now());
            sessionRepository.save(session);

            // Archive transcript to GCS
            if (session.getTranscript() != null && !session.getTranscript().isBlank()) {
                transcriptArchiveService.archiveTranscript(sessionId, session.getTranscript());
            }

            log.info("Closed session: {}", sessionId);
        });
    }

    @Transactional
    public void updateTranscript(String sessionId, String transcript) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            String existing = session.getTranscript() != null ? session.getTranscript() : "";
            session.setTranscript(existing + "\n" + transcript);
            sessionRepository.save(session);
        });
    }

    @Transactional
    public void updateTriageLevel(String sessionId, EmergencyLevel level, String reason) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setTriageLevel(level);
            session.setTriageReason(reason);
            sessionRepository.save(session);
            log.info("Session {} triage updated to {} — {}", sessionId, level, reason);
        });
    }

    @Transactional
    public void markError(String sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setStatus(SymptomSession.SessionStatus.ERROR);
            session.setClosedAt(LocalDateTime.now());
            sessionRepository.save(session);
        });
    }

    public Optional<SymptomSession> getSession(String sessionId) {
        return sessionRepository.findById(sessionId);
    }

    public List<SymptomSession> getActiveSessions() {
        return sessionRepository.findByStatusOrderByCreatedAtDesc(SymptomSession.SessionStatus.ACTIVE);
    }

    public List<SymptomSession> getAllSessions() {
        return sessionRepository.findAllByOrderByCreatedAtDesc();
    }
}
