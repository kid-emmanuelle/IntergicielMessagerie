package org.insa.intergicielmessagerie.service;

import org.insa.intergicielmessagerie.config.KafkaConfig;
import org.insa.intergicielmessagerie.dto.ChatMessageDTO;
import org.insa.intergicielmessagerie.model.Message;
import org.insa.intergicielmessagerie.model.User;
import org.insa.intergicielmessagerie.repository.MessageRepository;
import org.insa.intergicielmessagerie.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, ChatMessageDTO> kafkaTemplate;

    @Transactional
    public Message sendBroadcastMessage(String senderUsername, String content) {
        Optional<User> senderOpt = userRepository.findByUsername(senderUsername);

        if (senderOpt.isEmpty()) {
            throw new IllegalArgumentException("Sender not found: " + senderUsername);
        }

        User sender = senderOpt.get();

        // Check if user is allowed to send broadcast messages
        if (sender.getConnectionMode() != User.ConnectionMode.PUBLIC) {
            throw new IllegalStateException("User in " + sender.getConnectionMode() + " mode cannot send broadcast messages");
        }

        Message message = Message.builder()
                .sender(sender)
                .content(content)
                .type(Message.MessageType.TEXT)
                .broadcast(true)
                .build();

        message = messageRepository.save(message);

        // Send to Kafka
        ChatMessageDTO messageDTO = ChatMessageDTO.fromEntity(message);
        kafkaTemplate.send(KafkaConfig.TOPIC_BROADCAST, messageDTO);

        log.info("Broadcast message sent by: {}", senderUsername);
        return message;
    }

    @Transactional
    public Message sendPrivateMessage(String senderUsername, String receiverUsername, String content) {
        Optional<User> senderOpt = userRepository.findByUsername(senderUsername);
        Optional<User> receiverOpt = userRepository.findByUsername(receiverUsername);

        if (senderOpt.isEmpty()) {
            throw new IllegalArgumentException("Sender not found: " + senderUsername);
        }

        if (receiverOpt.isEmpty()) {
            throw new IllegalArgumentException("Receiver not found: " + receiverUsername);
        }

        User sender = senderOpt.get();
        User receiver = getUser(receiverOpt, sender);

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(content)
                .type(Message.MessageType.TEXT)
                .broadcast(false)
                .build();

        message = messageRepository.save(message);

        // Send to Kafka with receiver's username as key for proper routing
        ChatMessageDTO messageDTO = ChatMessageDTO.fromEntity(message);
        kafkaTemplate.send(KafkaConfig.TOPIC_PRIVATE, receiverUsername, messageDTO);

        log.info("Private message sent from {} to {}", senderUsername, receiverUsername);
        return message;
    }

    private static User getUser(Optional<User> receiverOpt, User sender) {
        User receiver = receiverOpt.get();

        // Check if user is allowed to send private messages
        if (sender.getConnectionMode() != User.ConnectionMode.PUBLIC) {
            throw new IllegalStateException("User in " + sender.getConnectionMode() + " mode cannot send private messages");
        }

        // Check if receiver is online and in PUBLIC mode
        if (!receiver.isOnline() || receiver.getConnectionMode() != User.ConnectionMode.PUBLIC) {
            throw new IllegalStateException("Cannot send private message to user who is offline or in PSEUDO_ANONYMOUS mode");
        }
        return receiver;
    }

    public List<ChatMessageDTO> getUserMessageHistory(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + username);
        }

        return messageRepository.findUserMessages(userOpt.get()).stream()
                .map(ChatMessageDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ChatMessageDTO> getBroadcastMessageHistory() {
        return messageRepository.findAllBroadcastMessages().stream()
                .map(ChatMessageDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ChatMessageDTO> getPrivateMessageHistory(String user1, String user2) {
        Optional<User> user1Opt = userRepository.findByUsername(user1);
        Optional<User> user2Opt = userRepository.findByUsername(user2);

        if (user1Opt.isEmpty() || user2Opt.isEmpty()) {
            throw new IllegalArgumentException("One or both users not found");
        }

        List<Message> messages = messageRepository.findMessagesBetweenUsers(
                user1Opt.get(), user2Opt.get());

        return messages.stream()
                .map(ChatMessageDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
