package org.insa.intergicielmessagerie.service;

import org.insa.intergicielmessagerie.config.KafkaConfig;
import org.insa.intergicielmessagerie.dto.ChatMessageDTO;
import org.insa.intergicielmessagerie.dto.FileEventDTO;
import org.insa.intergicielmessagerie.model.FileAttachment;
import org.insa.intergicielmessagerie.model.Message;
import org.insa.intergicielmessagerie.model.User;
import org.insa.intergicielmessagerie.repository.FileAttachmentRepository;
import org.insa.intergicielmessagerie.repository.MessageRepository;
import org.insa.intergicielmessagerie.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    @Value("${app.file.max-size}")
    private long maxFileSize;

    @Value("${app.file.default-expiry-hours}")
    private int defaultExpiryHours;

    private final FileAttachmentRepository fileRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, ChatMessageDTO> messageKafkaTemplate;
    private final KafkaTemplate<String, FileEventDTO> fileKafkaTemplate;

    /**
     * Initialize storage directory on startup
     */
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            log.info("Created file upload directory: {}", uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    @Transactional
    public FileAttachment storeFile(String senderUsername, String receiverUsername,
                                    MultipartFile file, Integer expiryHours) throws IOException {

        // Validate file size
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File exceeds maximum size of " + maxFileSize + " bytes");
        }

        Optional<User> senderOpt = userRepository.findByUsername(senderUsername);
        Optional<User> receiverOpt = userRepository.findByUsername(receiverUsername);

        if (senderOpt.isEmpty()) {
            throw new IllegalArgumentException("Sender not found: " + senderUsername);
        }

        if (receiverOpt.isEmpty()) {
            throw new IllegalArgumentException("Receiver not found: " + receiverUsername);
        }

        User sender = senderOpt.get();
        User receiver = receiverOpt.get();

        // Check if user is allowed to send files
        if (sender.getConnectionMode() != User.ConnectionMode.PUBLIC) {
            throw new IllegalStateException("User in " + sender.getConnectionMode() + " mode cannot send files");
        }

        // Check if receiver is online and in PUBLIC mode
        if (!receiver.isOnline() || receiver.getConnectionMode() != User.ConnectionMode.PUBLIC) {
            throw new IllegalStateException("Cannot send file to user who is offline or in PSEUDO_ANONYMOUS mode");
        }

        // Create a message for this file
        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content("File attachment: " + file.getOriginalFilename())
                .type(Message.MessageType.BINARY)
                .broadcast(false)
                .build();

        message = messageRepository.save(message);

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String storedFilename = UUID.randomUUID().toString() +
                (originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "");

        // Get file path
        Path targetPath = Paths.get(uploadDir).resolve(storedFilename);

        // Calculate expiry time
        LocalDateTime expiryTime = LocalDateTime.now().plusHours(expiryHours != null ? expiryHours : defaultExpiryHours);

        // Save file to disk
        Files.copy(file.getInputStream(), targetPath);

        // Create file metadata
        FileAttachment fileAttachment = FileAttachment.builder()
                .message(message)
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .filePath(targetPath.toString())
                .mimeType(file.getContentType())
                .fileSize(file.getSize())
                .expiryTime(expiryTime)
                .downloaded(false)
                .build();

        fileAttachment = fileRepository.save(fileAttachment);

        // Create ChatMessageDTO with file info
        ChatMessageDTO.FileInfo fileInfo = new ChatMessageDTO.FileInfo(
                fileAttachment.getId(),
                fileAttachment.getOriginalFilename(),
                fileAttachment.getFileSize(),
                fileAttachment.getExpiryTime()
        );

        ChatMessageDTO messageDTO = ChatMessageDTO.fromEntity(message);
        messageDTO.setFileInfo(fileInfo);

        // Send message notification to Kafka
        messageKafkaTemplate.send(KafkaConfig.TOPIC_PRIVATE, receiverUsername, messageDTO);

        // Send file event notification
        FileEventDTO fileEvent = FileEventDTO.fromEntity(fileAttachment, FileEventDTO.FileEventType.NEW_FILE_AVAILABLE);
        fileKafkaTemplate.send(KafkaConfig.TOPIC_FILE_TRANSFER, receiverUsername, fileEvent);

        log.info("File sent from {} to {}: {}", senderUsername, receiverUsername, originalFilename);
        return fileAttachment;
    }

    @Transactional
    public Resource downloadFile(Long fileId, String username) {
        Optional<FileAttachment> fileOpt = fileRepository.findById(fileId);

        if (fileOpt.isEmpty()) {
            throw new IllegalArgumentException("File not found with ID: " + fileId);
        }

        FileAttachment file = fileOpt.get();

        // Verify the user is the intended receiver
        User receiver = file.getMessage().getReceiver();
        if (receiver == null || !receiver.getUsername().equals(username)) {
            throw new IllegalStateException("User is not authorized to download this file");
        }

        // Check if file has expired
        if (LocalDateTime.now().isAfter(file.getExpiryTime())) {
            throw new IllegalStateException("File has expired");
        }

        // Mark as downloaded
        file.setDownloaded(true);
        fileRepository.save(file);

        // Send file event notification
        FileEventDTO fileEvent = FileEventDTO.fromEntity(file, FileEventDTO.FileEventType.FILE_DOWNLOADED);
        fileKafkaTemplate.send(KafkaConfig.TOPIC_FILE_TRANSFER, file.getMessage().getSender().getUsername(), fileEvent);

        try {
            Path filePath = Paths.get(file.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read the file");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    public List<FileEventDTO> getPendingFilesForUser(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + username);
        }

        return fileRepository.findPendingFilesByUser(userOpt.get()).stream()
                .map(file -> FileEventDTO.fromEntity(file, FileEventDTO.FileEventType.NEW_FILE_AVAILABLE))
                .collect(Collectors.toList());
    }

    /**
     * Scheduled task to clean up expired files
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void cleanupExpiredFiles() {
        LocalDateTime now = LocalDateTime.now();
        List<FileAttachment> expiredFiles = fileRepository.findExpiredFiles(now);

        for (FileAttachment file : expiredFiles) {
            try {
                // Delete physical file
                Files.deleteIfExists(Paths.get(file.getFilePath()));

                // Send file expired notification if not downloaded
                if (!file.isDownloaded()) {
                    FileEventDTO fileEvent = FileEventDTO.fromEntity(file, FileEventDTO.FileEventType.FILE_EXPIRED);
                    fileKafkaTemplate.send(KafkaConfig.TOPIC_FILE_TRANSFER,
                            file.getMessage().getReceiver().getUsername(), fileEvent);
                }

                // Remove from database
                fileRepository.delete(file);

            } catch (IOException e) {
                log.error("Error deleting expired file: {}", file.getFilePath(), e);
            }
        }

        log.info("Cleaned up {} expired files", expiredFiles.size());
    }
}
