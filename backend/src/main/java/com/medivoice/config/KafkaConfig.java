package com.medivoice.config;

import com.medivoice.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic sessionsTopic() {
        return TopicBuilder.name(KafkaTopics.TOPIC_SESSIONS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic symptomsTopic() {
        return TopicBuilder.name(KafkaTopics.TOPIC_SYMPTOMS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic triageTopic() {
        return TopicBuilder.name(KafkaTopics.TOPIC_TRIAGE)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic emergencyTopic() {
        return TopicBuilder.name(KafkaTopics.TOPIC_EMERGENCY)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
