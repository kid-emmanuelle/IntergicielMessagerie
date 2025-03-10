package org.insa.intergicielmessagerie.controller;

import org.insa.intergicielmessagerie.dto.ChatMessageDTO;
import org.insa.intergicielmessagerie.dto.UserStatusDTO;
import org.insa.intergicielmessagerie.model.User;
import org.insa.intergicielmessagerie.service.MessageService;
import org.insa.intergicielmessagerie.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {
    // Methods here
    private final UserService userService;
    private final MessageService messageService;

    /**
     * Handle user connection
     */
    @MessageMapping("/chat.connect")
    public void connect(@Payload Map<String, Object> connectionInfo,
                        SimpMessageHeaderAccessor headerAccessor) {
        String username = (String) connectionInfo.get("username");
        String modeStr = (String) connectionInfo.get("mode");

        // Stocker username dans la session WebSocket
        Objects.requireNonNull(headerAccessor.getSessionAttributes()).put("username", username);

        User.ConnectionMode mode = "PUBLIC".equalsIgnoreCase(modeStr)
                ? User.ConnectionMode.PUBLIC
                : User.ConnectionMode.PSEUDO_ANONYMOUS;

        // Store username in WebSocket session
        headerAccessor.getSessionAttributes().put("username", username);
        headerAccessor.getSessionAttributes().put("mode", mode.toString());

        // Connect user
        userService.connectUser(username, mode);

        log.info("User connected: {} in {} mode", username, mode);
    }

    /**
     * Handle broadcast messages
     */
    @MessageMapping("/chat.broadcast")
    public void sendBroadcast(@Payload ChatMessageDTO message, SimpMessageHeaderAccessor headerAccessor) {
        String username = (String) headerAccessor.getSessionAttributes().get("username");

        if (username == null) {
            throw new IllegalStateException("User not authenticated");
        }

        messageService.sendBroadcastMessage(username, message.getContent());

        log.info("Broadcast message received from {}", username);
    }

    /**
     * Handle private messages
     */
    @MessageMapping("/chat.private")
    public void sendPrivate(@Payload ChatMessageDTO message, SimpMessageHeaderAccessor headerAccessor) {
        String senderUsername = (String) headerAccessor.getSessionAttributes().get("username");

        if (senderUsername == null) {
            throw new IllegalStateException("User not authenticated");
        }

        String receiverUsername = message.getReceiver();

        messageService.sendPrivateMessage(senderUsername, receiverUsername, message.getContent());

        log.info("Private message received from {} to {}", senderUsername, receiverUsername);
    }
}
