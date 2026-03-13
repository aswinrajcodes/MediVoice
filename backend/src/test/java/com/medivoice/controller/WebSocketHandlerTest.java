package com.medivoice.controller;

import com.medivoice.model.SymptomSession;
import com.medivoice.service.GeminiLiveService;
import com.medivoice.service.KafkaProducerService;
import com.medivoice.service.SessionService;
import com.medivoice.service.TriageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketHandlerTest {

    @Mock private GeminiLiveService geminiLiveService;
    @Mock private TriageService triageService;
    @Mock private SessionService sessionService;
    @Mock private KafkaProducerService kafkaProducerService;
    @Mock private WebSocketSession wsSession;

    private WebSocketHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        handler = new WebSocketHandler(geminiLiveService, triageService, sessionService, kafkaProducerService);

        // Common session mock setup
        when(wsSession.getId()).thenReturn("ws-session-1");
        when(wsSession.isOpen()).thenReturn(true);
        when(wsSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws"));
        lenient().when(wsSession.getRemoteAddress())
                .thenReturn(new InetSocketAddress("127.0.0.1", 12345));
    }

    @Test
    @DisplayName("afterConnectionEstablished creates a session and initializes Gemini")
    void testConnectionEstablishedCreatesSession() throws Exception {
        SymptomSession session = new SymptomSession();
        session.setId("med-session-1");
        when(sessionService.createSession(anyString(), anyString())).thenReturn(session);

        handler.afterConnectionEstablished(wsSession);

        verify(sessionService).createSession(anyString(), anyString());
        verify(kafkaProducerService).publishSessionEvent(eq("med-session-1"), eq("SESSION_START"), anyString());
        verify(geminiLiveService).initSession(eq("med-session-1"), eq(wsSession), any(), any());
    }

    @Test
    @DisplayName("handleBinaryMessage forwards audio to Gemini")
    void testBinaryMessageForwardsToGemini() throws Exception {
        // First establish connection
        SymptomSession session = new SymptomSession();
        session.setId("med-session-2");
        when(sessionService.createSession(anyString(), anyString())).thenReturn(session);
        handler.afterConnectionEstablished(wsSession);

        // Then send binary audio
        byte[] audioData = new byte[]{0x01, 0x02, 0x03};
        BinaryMessage message = new BinaryMessage(audioData);

        handler.handleBinaryMessage(wsSession, message);

        verify(geminiLiveService).sendAudioChunk(eq("med-session-2"), any(byte[].class));
    }

    @Test
    @DisplayName("handleTextMessage with video_frame sends to Gemini")
    void testVideoFrameSendsToGemini() throws Exception {
        // First establish connection
        SymptomSession session = new SymptomSession();
        session.setId("med-session-3");
        when(sessionService.createSession(anyString(), anyString())).thenReturn(session);
        handler.afterConnectionEstablished(wsSession);

        // Send video frame
        String videoMsg = "{\"type\":\"video_frame\",\"data\":\"base64data\",\"mimeType\":\"image/jpeg\"}";
        handler.handleTextMessage(wsSession, new TextMessage(videoMsg));

        verify(geminiLiveService).sendVideoFrame(eq("med-session-3"), eq("base64data"), eq("image/jpeg"));
    }

    @Test
    @DisplayName("afterConnectionClosed cleans up resources")
    void testConnectionClosedCleansUp() throws Exception {
        // First establish connection
        SymptomSession session = new SymptomSession();
        session.setId("med-session-4");
        when(sessionService.createSession(anyString(), anyString())).thenReturn(session);
        handler.afterConnectionEstablished(wsSession);

        // Then close
        handler.afterConnectionClosed(wsSession, CloseStatus.NORMAL);

        verify(geminiLiveService).closeSession("med-session-4");
        verify(sessionService).closeSession("med-session-4");
        verify(kafkaProducerService).publishSessionEvent(eq("med-session-4"), eq("SESSION_END"), anyString());
    }
}
