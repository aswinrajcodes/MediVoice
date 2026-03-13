package com.medivoice.kafka;

public final class KafkaTopics {

    private KafkaTopics() {
        // Prevent instantiation
    }

    public static final String TOPIC_SESSIONS  = "medivoice.sessions";
    public static final String TOPIC_SYMPTOMS  = "medivoice.symptoms";
    public static final String TOPIC_TRIAGE    = "medivoice.triage";
    public static final String TOPIC_EMERGENCY = "medivoice.emergency";
}
