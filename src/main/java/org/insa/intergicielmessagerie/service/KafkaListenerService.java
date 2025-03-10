package org.insa.intergicielmessagerie.service;

import org.insa.intergicielmessagerie.config.KafkaConfig;
import org.insa.intergicielmessagerie.dto.ChatMessageDTO;
import org.insa.intergicielmessagerie.dto.FileEventDTO;
import org.insa.intergicielmessagerie.dto.UserStatusDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaListenerService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Listen for broadcast messages and forward to all connected clients
     */
    @KafkaListener(topics = KafkaConfig.TOPIC_BROADCAST, groupId = "chat-group")
    public void listenBroadcastMessages(ChatMessageDTO message) {
        log.debug("Received broadcast message: {}", message);

        // Forward to all subscribed clients via WebSocket
        messagingTemplate.convertAndSend("/topic/broadcast", message);
    }

    /**
     * Listen for private messages and forward to specific users
     */
    @KafkaListener(topics = KafkaConfig.TOPIC_PRIVATE, groupId = "chat-group")
    public void listenPrivateMessages(ChatMessageDTO message) {
        log.info("Received private message from {} to {}: {}",
                message.getSender(), message.getReceiver(), message.getContent());

        try {
            // Add log for debugging
            log.debug("Attempting to send to receiver: {} at destination: {}",
                    message.getReceiver(), "/user/" + message.getReceiver() + "/queue/messages");

            // Send to receiver
            messagingTemplate.convertAndSendToUser(
                    message.getReceiver(),
                    "/queue/messages",
                    message
            );

            // Send to sender
            if (!message.getSender().equals(message.getReceiver())) {
                log.debug("Attempting to send to sender: {} at destination: {}",
                        message.getSender(), "/user/" + message.getSender() + "/queue/messages");

                messagingTemplate.convertAndSendToUser(
                        message.getSender(),
                        "/queue/messages",
                        message
                );
            }
        } catch (Exception e) {
            log.error("Error sending message via WebSocket: {}", e.getMessage(), e);
        }
    }

    /**
     * Listen for user status changes and broadcast to all clients
     */
    @KafkaListener(topics = KafkaConfig.TOPIC_USER_STATUS, groupId = "chat-group")
    public void listenUserStatus(UserStatusDTO userStatus) {
        log.debug("Received user status update: {}", userStatus);

        // Only broadcast PUBLIC user status changes
        if (userStatus.getConnectionMode() == UserStatusDTO.ConnectionMode.PUBLIC) {
            messagingTemplate.convertAndSend("/topic/users", userStatus);
        }
    }

    /**
     * Listen for file transfer events and notify specific users
     */
    @KafkaListener(topics = KafkaConfig.TOPIC_FILE_TRANSFER, groupId = "chat-group")
    public void listenFileEvents(FileEventDTO fileEvent) {
        log.debug("Received file event: {}", fileEvent);

        // Forward to specific user via WebSocket
        switch (fileEvent.getEventType()) {
            case NEW_FILE_AVAILABLE:
                // Notify receiver about new file
                messagingTemplate.convertAndSendToUser(
                        fileEvent.getReceiver(),
                        "/queue/files",
                        fileEvent
                );
                break;

            case FILE_DOWNLOADED:
                // Notify sender that file was downloaded
                messagingTemplate.convertAndSendToUser(
                        fileEvent.getSender(),
                        "/queue/files",
                        fileEvent
                );
                break;

            case FILE_EXPIRED:
                // Notify receiver that file expired
                messagingTemplate.convertAndSendToUser(
                        fileEvent.getReceiver(),
                        "/queue/files",
                        fileEvent
                );
                break;
        }
    }
}
