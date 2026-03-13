package com.medivoice.service;

import com.medivoice.model.SymptomSession;
import com.medivoice.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private TranscriptArchiveService transcriptArchiveService;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(sessionRepository, transcriptArchiveService);
    }

    @Test
    @DisplayName("createSession saves a new session with ACTIVE status")
    void testCreateSession() {
        when(sessionRepository.save(any(SymptomSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SymptomSession session = sessionService.createSession("TestAgent", "127.0.0.1");

        assertNotNull(session);
        assertNotNull(session.getId());
        assertEquals(SymptomSession.SessionStatus.ACTIVE, session.getStatus());
        assertEquals("TestAgent", session.getUserAgent());
        assertEquals("127.0.0.1", session.getRemoteAddress());
        verify(sessionRepository).save(any(SymptomSession.class));
    }

    @Test
    @DisplayName("closeSession sets status to CLOSED and archives transcript")
    void testCloseSession() {
        SymptomSession session = new SymptomSession();
        session.setId("session-123");
        session.setStatus(SymptomSession.SessionStatus.ACTIVE);
        session.setTranscript("Patient reported headache");

        when(sessionRepository.findById("session-123")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(SymptomSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        sessionService.closeSession("session-123");

        assertEquals(SymptomSession.SessionStatus.CLOSED, session.getStatus());
        assertNotNull(session.getClosedAt());
        verify(sessionRepository).save(session);
        verify(transcriptArchiveService).archiveTranscript("session-123", "Patient reported headache");
    }

    @Test
    @DisplayName("closeSession does nothing for nonexistent session")
    void testCloseNonexistentSession() {
        when(sessionRepository.findById("nonexistent")).thenReturn(Optional.empty());

        sessionService.closeSession("nonexistent");

        verify(sessionRepository, never()).save(any());
        verify(transcriptArchiveService, never()).archiveTranscript(any(), any());
    }

    @Test
    @DisplayName("updateTranscript appends text to existing transcript")
    void testUpdateTranscript() {
        SymptomSession session = new SymptomSession();
        session.setId("session-123");
        session.setTranscript("Initial text");

        when(sessionRepository.findById("session-123")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(SymptomSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        sessionService.updateTranscript("session-123", " Additional text");

        assertTrue(session.getTranscript().contains("Additional text"));
        verify(sessionRepository).save(session);
    }

    @Test
    @DisplayName("updateTriageLevel updates triage level and reason")
    void testUpdateTriageLevel() {
        SymptomSession session = new SymptomSession();
        session.setId("session-123");

        when(sessionRepository.findById("session-123")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(SymptomSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        sessionService.updateTriageLevel("session-123", "HIGH", "Chest pain detected");

        assertEquals("HIGH", session.getTriageLevel());
        assertEquals("Chest pain detected", session.getTriageReason());
        verify(sessionRepository).save(session);
    }

    @Test
    @DisplayName("getActiveSessions returns only ACTIVE sessions")
    void testGetActiveSessions() {
        SymptomSession active = new SymptomSession();
        active.setStatus(SymptomSession.SessionStatus.ACTIVE);

        when(sessionRepository.findByStatus(SymptomSession.SessionStatus.ACTIVE))
                .thenReturn(List.of(active));

        List<SymptomSession> result = sessionService.getActiveSessions();

        assertEquals(1, result.size());
        assertEquals(SymptomSession.SessionStatus.ACTIVE, result.get(0).getStatus());
    }
}
