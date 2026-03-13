package com.medivoice.service;

import com.medivoice.kafka.KafkaTopics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private KafkaProducerService kafkaProducerService;

    @Test
    @DisplayName("publishSessionEvent sends message to sessions topic")
    void testPublishSessionEvent() {
        kafkaProducerService.publishSessionEvent("session-1", "SESSION_START", "{}");

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        assertEquals(KafkaTopics.SESSIONS, topicCaptor.getValue());
        assertEquals("session-1", keyCaptor.getValue());
        assertTrue(valueCaptor.getValue().contains("SESSION_START"));
        assertTrue(valueCaptor.getValue().contains("session-1"));
    }

    @Test
    @DisplayName("publishTriageEvent sends message to triage topic")
    void testPublishTriageEvent() {
        kafkaProducerService.publishTriageEvent("session-2", "TRIAGE_EVALUATION", "{\"level\":\"HIGH\"}");

        verify(kafkaTemplate).send(eq(KafkaTopics.TRIAGE), eq("session-2"), anyString());
    }

    @Test
    @DisplayName("publishEmergencyEvent sends message to emergency topic")
    void testPublishEmergencyEvent() {
        kafkaProducerService.publishEmergencyEvent("session-3", "EMERGENCY_DETECTED", "{\"reason\":\"Chest pain\"}");

        verify(kafkaTemplate).send(eq(KafkaTopics.EMERGENCY), eq("session-3"), anyString());
    }
}
