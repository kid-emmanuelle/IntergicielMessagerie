package org.insa.intergicielmessagerie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.insa.intergicielmessagerie.model.Message;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {

    private Long id;
    private String sender;
    private String receiver; // Null for broadcast
    private String content;
    private MessageType type;
    private boolean broadcast;
    private LocalDateTime timestamp;
    private FileInfo fileInfo; // Included only for BINARY messages

    public enum MessageType {
        TEXT, BINARY
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        private Long id;
        private String filename;
        private long fileSize;
        private LocalDateTime expiryTime;
    }

    public static ChatMessageDTO fromEntity(Message message) {
        ChatMessageDTO dto = ChatMessageDTO.builder()
                .id(message.getId())
                .sender(message.getSender().getUsername())
                .content(message.getContent())
                .type(message.getType() == Message.MessageType.TEXT ?
                        MessageType.TEXT : MessageType.BINARY)
                .broadcast(message.isBroadcast())
                .timestamp(message.getTimestamp())
                .build();

        if (message.getReceiver() != null) {
            dto.setReceiver(message.getReceiver().getUsername());
        }

        return dto;
    }
}
