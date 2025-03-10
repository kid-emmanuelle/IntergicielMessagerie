package org.insa.intergicielmessagerie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.insa.intergicielmessagerie.model.FileAttachment;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileEventDTO {

    private Long id;
    private String sender;
    private String receiver;
    private String filename;
    private long fileSize;
    private LocalDateTime uploadTime;
    private LocalDateTime expiryTime;
    private FileEventType eventType;

    public enum FileEventType {
        NEW_FILE_AVAILABLE,
        FILE_DOWNLOADED,
        FILE_EXPIRED
    }

    public static FileEventDTO fromEntity(FileAttachment file, FileEventType eventType) {
        return FileEventDTO.builder()
                .id(file.getId())
                .sender(file.getMessage().getSender().getUsername())
                .receiver(file.getMessage().getReceiver().getUsername())
                .filename(file.getOriginalFilename())
                .fileSize(file.getFileSize())
                .uploadTime(file.getUploadTime())
                .expiryTime(file.getExpiryTime())
                .eventType(eventType)
                .build();
    }
}
