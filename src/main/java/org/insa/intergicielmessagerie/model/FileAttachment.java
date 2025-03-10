package org.insa.intergicielmessagerie.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_attachments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "message_id")
    private Message message;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String storedFilename;

    @Column(nullable = false)
    private String filePath;

    private String mimeType;

    private long fileSize;

    private LocalDateTime uploadTime;

    private LocalDateTime expiryTime;

    private boolean downloaded;

    @PrePersist
    protected void onCreate() {
        uploadTime = LocalDateTime.now();
    }
}