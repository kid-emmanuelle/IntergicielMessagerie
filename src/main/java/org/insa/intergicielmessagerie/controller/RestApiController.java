package org.insa.intergicielmessagerie.controller;

import org.insa.intergicielmessagerie.dto.ChatMessageDTO;
import org.insa.intergicielmessagerie.dto.FileEventDTO;
import org.insa.intergicielmessagerie.dto.UserStatusDTO;
import org.insa.intergicielmessagerie.model.User;
import org.insa.intergicielmessagerie.service.FileService;
import org.insa.intergicielmessagerie.service.MessageService;
import org.insa.intergicielmessagerie.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class RestApiController {

    private final UserService userService;
    private final MessageService messageService;
    private final FileService fileService;

    /**
     * Connect user via REST API
     */
    @PostMapping("/users/connect")
    public ResponseEntity<UserStatusDTO> connectUser(@RequestBody Map<String, String> connectionInfo) {
        String username = connectionInfo.get("username");
        String modeStr = connectionInfo.get("mode");

        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        User.ConnectionMode mode = "PUBLIC".equalsIgnoreCase(modeStr)
                ? User.ConnectionMode.PUBLIC
                : User.ConnectionMode.PSEUDO_ANONYMOUS;

        User user = userService.connectUser(username, mode);
        return ResponseEntity.ok(UserStatusDTO.fromEntity(user));
    }

    /**
     * Disconnect user via REST API
     */
    @PostMapping("/users/disconnect")
    public ResponseEntity<Void> disconnectUser(@RequestBody Map<String, String> disconnectInfo) {
        String username = disconnectInfo.get("username");

        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        userService.disconnectUser(username);
        return ResponseEntity.ok().build();
    }

    /**
     * Get list of online users
     */
    @GetMapping("/users/online")
    public ResponseEntity<List<UserStatusDTO>> getOnlineUsers() {
        return ResponseEntity.ok(userService.getOnlineUsers());
    }

    /**
     * Get message history for a user
     */
    @GetMapping("/messages/history")
    public ResponseEntity<List<ChatMessageDTO>> getMessageHistory(@RequestParam String username) {
        return ResponseEntity.ok(messageService.getUserMessageHistory(username));
    }

    /**
     * Get broadcast message history
     */
    @GetMapping("/messages/broadcast")
    public ResponseEntity<List<ChatMessageDTO>> getBroadcastHistory() {
        return ResponseEntity.ok(messageService.getBroadcastMessageHistory());
    }

    /**
     * Get private message history between two users
     */
    @GetMapping("/messages/private")
    public ResponseEntity<List<ChatMessageDTO>> getPrivateHistory(
            @RequestParam String user1,
            @RequestParam String user2) {
        return ResponseEntity.ok(messageService.getPrivateMessageHistory(user1, user2));
    }

    /**
     * Upload file attachment
     */
    @PostMapping("/files/upload")
    public ResponseEntity<FileEventDTO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sender") String sender,
            @RequestParam("receiver") String receiver,
            @RequestParam(value = "expiryHours", required = false) Integer expiryHours) {

        try {
            var fileAttachment = fileService.storeFile(sender, receiver, file, expiryHours);
            var fileEvent = FileEventDTO.fromEntity(fileAttachment, FileEventDTO.FileEventType.NEW_FILE_AVAILABLE);
            return ResponseEntity.ok(fileEvent);
        } catch (IOException e) {
            log.error("Could not upload file", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Download file attachment
     */
    @GetMapping("/files/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long fileId,
            @RequestParam String username) {

        try {
            Resource file = fileService.downloadFile(fileId, username);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                    .body(file);
        } catch (Exception e) {
            log.error("Could not download file", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get pending files for a user
     */
    @GetMapping("/files/pending")
    public ResponseEntity<List<FileEventDTO>> getPendingFiles(@RequestParam String username) {
        return ResponseEntity.ok(fileService.getPendingFilesForUser(username));
    }
}
