package com.medivoice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Gemini Live API integration service using raw WebSocket connection.
 * Connects to the Gemini BidiGenerateContent WebSocket endpoint,
 * streams audio/video in real-time, and receives audio responses.
 */
@Service
@Slf4j
public class GeminiLiveService {

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash-native-audio-preview}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Active Gemini sessions keyed by sessionId
    private final Map<String, GeminiSession> activeSessions = new ConcurrentHashMap<>();

    private static final String SYSTEM_PROMPT = """
            You are MediVoice, a calm, compassionate, and knowledgeable AI health assistant.
            Your purpose is to help users understand their symptoms clearly and safely.

            PERSONALITY:
            - Speak like a calm, reassuring doctor friend — never clinical or cold
            - Use simple, plain language — no medical jargon without immediate explanation
            - Be warm and reassuring, but honest about the limits of AI guidance

            CONVERSATION RULES:
            1. Listen carefully to all symptoms before suggesting anything
            2. Ask ONE clarifying question at a time:
               Examples: "How long have you had this?", "Is the pain constant or does it come and go?",
               "On a scale of 1 to 10, how severe is the pain?"
            3. When the user shows you something on camera, describe exactly what you observe first
            4. Always acknowledge interruptions naturally: "Of course, tell me more about that..."
            5. After giving guidance, always end with:
               "Remember, I am an AI assistant — please consult a real doctor for proper diagnosis."

            VISION RULES:
            - Skin condition: describe color, texture, spread, and notable features
            - Pill bottle: read the label, identify the medication, explain its purpose
            - Wound: assess visible severity and give first aid guidance
            - Medical image: acknowledge it but recommend professional interpretation

            EMERGENCY PROTOCOL — CRITICAL:
            If the user mentions OR if you observe ANY of these:
            chest pain, difficulty breathing, signs of stroke, severe bleeding,
            loss of consciousness, severe allergic reaction, poisoning, overdose,
            thoughts of self-harm — immediately say:
            "I need to stop you right there — what you are describing sounds like a medical emergency.
            Please call 911 immediately or have someone take you to the nearest emergency room right now."
            Then stop all other guidance.

            NEVER:
            - Diagnose definitively — say "this could be..." never "you have..."
            - Recommend specific prescription medications or dosages
            - Minimize symptoms to make someone feel better
            """;

    /**
     * Initialize a Gemini Live API streaming session.
     * Opens a WebSocket to the Gemini BidiGenerateContent endpoint.
     */
    public void initSession(String sessionId, WebSocketSession wsSession,
                             BiConsumer<String, byte[]> onAudioResponse,
                             BiConsumer<String, String> onTranscriptComplete) {

        String wsUrl = String.format(
                "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=%s",
                apiKey
        );

        try {
            GeminiWebSocketClient geminiClient = new GeminiWebSocketClient(
                    new URI(wsUrl), sessionId, wsSession,
                    onAudioResponse, onTranscriptComplete, objectMapper, model
            );

            GeminiSession geminiSession = new GeminiSession(geminiClient, wsSession);
            activeSessions.put(sessionId, geminiSession);

            geminiClient.connect();
            log.info("Gemini Live session initiating for sessionId: {}", sessionId);

        } catch (Exception e) {
            log.error("Failed to create Gemini Live session for {}: {}", sessionId, e.getMessage(), e);
        }
    }

    /**
     * Send raw PCM16 audio chunk to Gemini Live API.
     */
    public void sendAudioChunk(String sessionId, byte[] audioData) {
        GeminiSession session = activeSessions.get(sessionId);
        if (session == null || !session.client.isOpen()) {
            log.warn("No active Gemini session for {}", sessionId);
            return;
        }

        try {
            String base64Audio = Base64.getEncoder().encodeToString(audioData);

            ObjectNode message = objectMapper.createObjectNode();
            ObjectNode realtimeInput = objectMapper.createObjectNode();
            ObjectNode mediaChunks = objectMapper.createObjectNode();
            mediaChunks.put("mimeType", "audio/pcm;rate=16000");
            mediaChunks.put("data", base64Audio);

            ArrayNode mediaArray = objectMapper.createArrayNode();
            mediaArray.add(mediaChunks);
            realtimeInput.set("mediaChunks", mediaArray);
            message.set("realtimeInput", realtimeInput);

            session.client.send(objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            log.error("Error sending audio to Gemini for session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Send a video frame (JPEG image) to Gemini Live API.
     */
    public void sendVideoFrame(String sessionId, byte[] imageData, String mimeType) {
        GeminiSession session = activeSessions.get(sessionId);
        if (session == null || !session.client.isOpen()) {
            return;
        }

        try {
            String base64Image = Base64.getEncoder().encodeToString(imageData);

            ObjectNode message = objectMapper.createObjectNode();
            ObjectNode realtimeInput = objectMapper.createObjectNode();
            ObjectNode mediaChunks = objectMapper.createObjectNode();
            mediaChunks.put("mimeType", mimeType != null ? mimeType : "image/jpeg");
            mediaChunks.put("data", base64Image);

            ArrayNode mediaArray = objectMapper.createArrayNode();
            mediaArray.add(mediaChunks);
            realtimeInput.set("mediaChunks", mediaArray);
            message.set("realtimeInput", realtimeInput);

            session.client.send(objectMapper.writeValueAsString(message));
            log.debug("Sent video frame to Gemini for session {}", sessionId);
        } catch (Exception e) {
            log.error("Error sending video frame to Gemini for session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Signal Gemini to stop responding (barge-in / interruption).
     */
    public void interrupt(String sessionId) {
        // Clear any pending audio on the client side
        // The Gemini Live API handles barge-in automatically when new audio arrives
        GeminiSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.interrupted = true;
            log.info("Interrupt signaled for session {}", sessionId);
        }
    }

    /**
     * Close the Gemini Live session cleanly.
     */
    public void closeSession(String sessionId) {
        GeminiSession session = activeSessions.remove(sessionId);
        if (session != null && session.client.isOpen()) {
            session.client.close();
            log.info("Gemini session closed for {}", sessionId);
        }
    }

    public boolean isSessionActive(String sessionId) {
        GeminiSession session = activeSessions.get(sessionId);
        return session != null && session.client.isOpen();
    }

    // ─── Inner classes ──────────────────────────────────────────────

    private static class GeminiSession {
        final GeminiWebSocketClient client;
        final WebSocketSession browserSession;
        volatile boolean interrupted = false;

        GeminiSession(GeminiWebSocketClient client, WebSocketSession browserSession) {
            this.client = client;
            this.browserSession = browserSession;
        }
    }

    /**
     * WebSocket client for the Gemini Live API BidiGenerateContent endpoint.
     */
    private static class GeminiWebSocketClient extends WebSocketClient {

        private final String sessionId;
        private final WebSocketSession browserSession;
        private final BiConsumer<String, byte[]> onAudioResponse;
        private final BiConsumer<String, String> onTranscriptComplete;
        private final ObjectMapper objectMapper;
        private final String modelName;
        private final StringBuilder transcriptBuilder = new StringBuilder();

        GeminiWebSocketClient(URI serverUri, String sessionId,
                              WebSocketSession browserSession,
                              BiConsumer<String, byte[]> onAudioResponse,
                              BiConsumer<String, String> onTranscriptComplete,
                              ObjectMapper objectMapper, String modelName) {
            super(serverUri);
            this.sessionId = sessionId;
            this.browserSession = browserSession;
            this.onAudioResponse = onAudioResponse;
            this.onTranscriptComplete = onTranscriptComplete;
            this.objectMapper = objectMapper;
            this.modelName = modelName;
        }

        @Override
        public void onOpen(ServerHandshake handshake) {
            log.info("Gemini WebSocket connected for session {}", sessionId);
            sendSetupMessage();
        }

        @Override
        public void onMessage(String message) {
            try {
                JsonNode response = objectMapper.readTree(message);

                // Handle server content (audio response)
                if (response.has("serverContent")) {
                    JsonNode serverContent = response.get("serverContent");

                    if (serverContent.has("modelTurn")) {
                        JsonNode modelTurn = serverContent.get("modelTurn");
                        if (modelTurn.has("parts")) {
                            for (JsonNode part : modelTurn.get("parts")) {
                                // Handle inline audio data
                                if (part.has("inlineData")) {
                                    JsonNode inlineData = part.get("inlineData");
                                    String mimeType = inlineData.has("mimeType") ?
                                            inlineData.get("mimeType").asText() : "";

                                    if (mimeType.startsWith("audio/")) {
                                        String base64Audio = inlineData.get("data").asText();
                                        byte[] audioBytes = Base64.getDecoder().decode(base64Audio);
                                        onAudioResponse.accept(sessionId, audioBytes);
                                    }
                                }
                                // Handle text transcript
                                if (part.has("text")) {
                                    transcriptBuilder.append(part.get("text").asText());
                                }
                            }
                        }
                    }

                    // Check if this is the end of the turn
                    boolean turnComplete = serverContent.has("turnComplete") &&
                            serverContent.get("turnComplete").asBoolean();
                    if (turnComplete && transcriptBuilder.length() > 0) {
                        String transcript = transcriptBuilder.toString();
                        transcriptBuilder.setLength(0); // reset
                        onTranscriptComplete.accept(sessionId, transcript);
                    }
                }

                // Handle setup complete
                if (response.has("setupComplete")) {
                    log.info("Gemini setup complete for session {}", sessionId);
                }

            } catch (Exception e) {
                log.error("Error processing Gemini response for session {}: {}", sessionId, e.getMessage());
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            log.info("Gemini WebSocket closed for session {} — code: {}, reason: {}, remote: {}",
                    sessionId, code, reason, remote);
        }

        @Override
        public void onError(Exception ex) {
            log.error("Gemini WebSocket error for session {}: {}", sessionId, ex.getMessage());
        }

        /**
         * Send the initial setup message with system prompt and audio configuration.
         */
        private void sendSetupMessage() {
            try {
                ObjectNode setup = objectMapper.createObjectNode();
                ObjectNode setupConfig = objectMapper.createObjectNode();

                // Model specification — use injected config
                setupConfig.put("model", "models/" + modelName);

                // Generation config
                ObjectNode generationConfig = objectMapper.createObjectNode();

                // Response modalities — audio only
                ArrayNode responseModalities = objectMapper.createArrayNode();
                responseModalities.add("AUDIO");
                generationConfig.set("responseModalities", responseModalities);

                // Speech config
                ObjectNode speechConfig = objectMapper.createObjectNode();
                ObjectNode voiceConfig = objectMapper.createObjectNode();
                voiceConfig.put("prebuiltVoiceConfig", "Aoede");
                speechConfig.set("voiceConfig", voiceConfig);
                generationConfig.set("speechConfig", speechConfig);

                // Output audio config
                ObjectNode audioConfig = objectMapper.createObjectNode();
                audioConfig.put("audioEncoding", "LINEAR16");
                audioConfig.put("sampleRateHertz", 24000);
                generationConfig.set("audioConfig", audioConfig);

                // Input audio config
                ObjectNode inputAudioConfig = objectMapper.createObjectNode();
                inputAudioConfig.put("audioEncoding", "LINEAR16");
                inputAudioConfig.put("sampleRateHertz", 16000);
                generationConfig.set("inputAudioConfig", inputAudioConfig);

                setupConfig.set("generationConfig", generationConfig);

                // System instruction
                ObjectNode systemInstruction = objectMapper.createObjectNode();
                ArrayNode parts = objectMapper.createArrayNode();
                ObjectNode textPart = objectMapper.createObjectNode();
                textPart.put("text", SYSTEM_PROMPT);
                parts.add(textPart);
                systemInstruction.set("parts", parts);
                setupConfig.set("systemInstruction", systemInstruction);

                setup.set("setup", setupConfig);

                send(objectMapper.writeValueAsString(setup));
                log.info("Sent setup message to Gemini for session {}", sessionId);

            } catch (Exception e) {
                log.error("Failed to send setup message to Gemini: {}", e.getMessage());
            }
        }
    }

}
