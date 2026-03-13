package com.medivoice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medivoice.kafka.KafkaTopics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void publishSessionEvent(String sessionId, String eventType, Object payload) {
        publish(KafkaTopics.TOPIC_SESSIONS, sessionId, eventType, payload);
    }

    public void publishSymptomEvent(String sessionId, String eventType, Object payload) {
        publish(KafkaTopics.TOPIC_SYMPTOMS, sessionId, eventType, payload);
    }

    public void publishTriageEvent(String sessionId, String eventType, Object payload) {
        publish(KafkaTopics.TOPIC_TRIAGE, sessionId, eventType, payload);
    }

    public void publishEmergencyEvent(String sessionId, String eventType, Object payload) {
        publish(KafkaTopics.TOPIC_EMERGENCY, sessionId, eventType, payload);
    }

    private void publish(String topic, String sessionId, String eventType, Object payload) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("sessionId", sessionId);
            message.put("timestamp", Instant.now().toString());
            message.put("eventType", eventType);
            message.put("payload", payload);

            String json = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topic, sessionId, json)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish to {}: {}", topic, ex.getMessage());
                    } else {
                        log.debug("Published to {} [partition={}, offset={}]",
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
        } catch (Exception e) {
            log.error("Error serializing Kafka message for topic {}: {}", topic, e.getMessage());
        }
    }
}
