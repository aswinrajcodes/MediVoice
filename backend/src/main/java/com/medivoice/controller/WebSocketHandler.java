package com.medivoice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medivoice.model.EmergencyLevel;
import com.medivoice.model.SymptomSession;
import com.medivoice.model.TriageResult;
import com.medivoice.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core WebSocket handler — the central nervous system of MediVoice.
 * Bridges the browser client with Gemini Live API and orchestrates
 * session management, triage evaluation, and Kafka event streaming.
 */
@Component
@Slf4j
public class WebSocketHandler extends AbstractWebSocketHandler {

    private final GeminiLiveService geminiLiveService;
    private final TriageService triageService;
    private final SessionService sessionService;
    private final KafkaProducerService kafkaService;
    private final ObjectMapper objectMapper;

    // Map WebSocket session ID → MediVoice session ID
    private final Map<String, String> sessionMapping = new ConcurrentHashMap<>();

    public WebSocketHandler(GeminiLiveService geminiLiveService,
                            TriageService triageService,
                            SessionService sessionService,
                            KafkaProducerService kafkaService) {
        this.geminiLiveService = geminiLiveService;
        this.triageService = triageService;
        this.sessionService = sessionService;
        this.kafkaService = kafkaService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession wsSession) throws Exception {
        log.info("WebSocket connection established: {}", wsSession.getId());

        // Create a new session in PostgreSQL
        String userAgent = wsSession.getHandshakeHeaders().getFirst("User-Agent");
        String remoteAddress = wsSession.getRemoteAddress() != null ?
                wsSession.getRemoteAddress().toString() : "unknown";

        SymptomSession session = sessionService.createSession(userAgent, remoteAddress);
        String sessionId = session.getId();
        sessionMapping.put(wsSession.getId(), sessionId);

        // Publish SESSION_START event to Kafka
        kafkaService.publishSessionEvent(sessionId, "SESSION_START",
                Map.of("wsSessionId", wsSession.getId(), "remoteAddress", remoteAddress));

        // Initialize Gemini Live API streaming session
        geminiLiveService.initSession(sessionId, wsSession,
                // onAudioResponse callback: forward Gemini audio to browser
                (sid, audioData) -> {
                    try {
                        if (wsSession.isOpen()) {
                            wsSession.sendMessage(new BinaryMessage(ByteBuffer.wrap(audioData)));
                        }
                    } catch (IOException e) {
                        log.error("Failed to send audio to browser for session {}: {}", sid, e.getMessage());
                    }
                },
                // onTranscriptComplete callback: send transcript + run triage
                (sid, transcript) -> {
                    try {
                        handleTranscriptComplete(wsSession, sid, transcript);
                    } catch (Exception e) {
                        log.error("Error handling transcript for session {}: {}", sid, e.getMessage());
                    }
                }
        );

        // Send welcome message to client
        sendJsonMessage(wsSession, Map.of(
                "type", "status",
                "message", "MediVoice connected. How can I help you today?"
        ));
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession wsSession, BinaryMessage message) {
        String sessionId = sessionMapping.get(wsSession.getId());
        if (sessionId == null) return;

        // Forward raw audio chunk directly to Gemini — no buffering for minimum latency
        byte[] audioData = message.getPayload().array();
        geminiLiveService.sendAudioChunk(sessionId, audioData);
    }

    @Override
    protected void handleTextMessage(WebSocketSession wsSession, TextMessage message) throws Exception {
        String sessionId = sessionMapping.get(wsSession.getId());
        if (sessionId == null) return;

        JsonNode json = objectMapper.readTree(message.getPayload());
        String type = json.has("type") ? json.get("type").asText() : "";

        switch (type) {
            case "video_frame" -> {
                // Decode base64 image data and forward to Gemini
                String base64Data = json.get("data").asText();
                byte[] imageData = Base64.getDecoder().decode(base64Data);
                String mimeType = json.has("mimeType") ? json.get("mimeType").asText() : "image/jpeg";
                geminiLiveService.sendVideoFrame(sessionId, imageData, mimeType);
                log.debug("Forwarded video frame to Gemini for session {}", sessionId);
            }

            case "interrupt" -> {
                geminiLiveService.interrupt(sessionId);
                log.info("Interrupt received for session {}", sessionId);
            }

            case "session_end" -> {
                geminiLiveService.closeSession(sessionId);
                sessionService.closeSession(sessionId);
                kafkaService.publishSessionEvent(sessionId, "SESSION_END",
                        Map.of("reason", "client_requested"));
                log.info("Session ended by client: {}", sessionId);
            }

            case "transcript" -> {
                // User-side transcript from speech recognition (if used)
                String text = json.has("text") ? json.get("text").asText() : "";
                if (!text.isBlank()) {
                    sessionService.updateTranscript(sessionId, "USER: " + text);
                    // Send back to display
                    sendJsonMessage(wsSession, Map.of(
                            "type", "transcript",
                            "role", "user",
                            "text", text
                    ));
                    kafkaService.publishSymptomEvent(sessionId, "USER_TRANSCRIPT",
                            Map.of("text", text));
                }
            }

            default -> log.warn("Unknown message type from client: {}", type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession wsSession, CloseStatus status) {
        String sessionId = sessionMapping.remove(wsSession.getId());
        if (sessionId != null) {
            geminiLiveService.closeSession(sessionId);
            sessionService.closeSession(sessionId);
            kafkaService.publishSessionEvent(sessionId, "SESSION_END",
                    Map.of("reason", "connection_closed", "closeCode", status.getCode()));
            log.info("Session cleaned up on connection close: {} (code: {})", sessionId, status.getCode());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession wsSession, Throwable exception) {
        String sessionId = sessionMapping.get(wsSession.getId());
        log.error("WebSocket transport error for session {}: {}", sessionId, exception.getMessage());
        if (sessionId != null) {
            sessionService.markError(sessionId);
        }
    }

    /**
     * Handle completed transcript from Gemini: display it, run triage, and publish events.
     */
    private void handleTranscriptComplete(WebSocketSession wsSession, String sessionId, String transcript)
            throws IOException {

        // Send transcript to browser
        sendJsonMessage(wsSession, Map.of(
                "type", "transcript",
                "role", "assistant",
                "text", transcript
        ));

        // Persist transcript
        sessionService.updateTranscript(sessionId, "ASSISTANT: " + transcript);

        // Run triage evaluation
        TriageResult triageResult = triageService.evaluate(transcript);

        // Publish triage event to Kafka
        kafkaService.publishTriageEvent(sessionId, "TRIAGE_EVALUATION", Map.of(
                "transcript", transcript,
                "severity", triageResult.getSeverity().name(),
                "reason", triageResult.getReason() != null ? triageResult.getReason() : ""
        ));

        // If HIGH or MEDIUM severity, send emergency alert to browser
        if (triageResult.getSeverity() == EmergencyLevel.HIGH) {
            sendJsonMessage(wsSession, Map.of(
                    "type", "emergency",
                    "level", "high",
                    "reason", triageResult.getReason()
            ));
            sessionService.updateTriageLevel(sessionId, EmergencyLevel.HIGH, triageResult.getReason());
            kafkaService.publishEmergencyEvent(sessionId, "EMERGENCY_HIGH", Map.of(
                    "reason", triageResult.getReason(),
                    "transcript", transcript
            ));
            log.warn("🚨 HIGH EMERGENCY detected for session {}: {}", sessionId, triageResult.getReason());

        } else if (triageResult.getSeverity() == EmergencyLevel.MEDIUM) {
            sendJsonMessage(wsSession, Map.of(
                    "type", "emergency",
                    "level", "medium",
                    "reason", triageResult.getReason()
            ));
            sessionService.updateTriageLevel(sessionId, EmergencyLevel.MEDIUM, triageResult.getReason());
            log.info("⚠️ MEDIUM alert for session {}: {}", sessionId, triageResult.getReason());
        }
    }

    private void sendJsonMessage(WebSocketSession wsSession, Object payload) throws IOException {
        if (wsSession.isOpen()) {
            String json = objectMapper.writeValueAsString(payload);
            wsSession.sendMessage(new TextMessage(json));
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
