package org.insa.intergicielmessagerie.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Kafka topic names as constants
    public static final String TOPIC_BROADCAST = "broadcast-topic";
    public static final String TOPIC_PRIVATE = "private-topic";
    public static final String TOPIC_USER_STATUS = "user-status-topic";
    public static final String TOPIC_FILE_TRANSFER = "file-transfer-topic";

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic broadcastTopic() {
        return new NewTopic(TOPIC_BROADCAST, 1, (short) 1);
    }

    @Bean
    public NewTopic privateTopic() {
        return new NewTopic(TOPIC_PRIVATE, 3, (short) 1); // More partitions for private messages
    }

    @Bean
    public NewTopic userStatusTopic() {
        return new NewTopic(TOPIC_USER_STATUS, 1, (short) 1);
    }

    @Bean
    public NewTopic fileTransferTopic() {
        return new NewTopic(TOPIC_FILE_TRANSFER, 1, (short) 1);
    }
}