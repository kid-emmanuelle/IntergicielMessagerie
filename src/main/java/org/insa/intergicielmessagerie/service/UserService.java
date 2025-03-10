package org.insa.intergicielmessagerie.service;

import org.insa.intergicielmessagerie.config.KafkaConfig;
import org.insa.intergicielmessagerie.dto.UserStatusDTO;
import org.insa.intergicielmessagerie.model.User;
import org.insa.intergicielmessagerie.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final KafkaTemplate<String, UserStatusDTO> kafkaTemplate;

    @Transactional
    public User connectUser(String username, User.ConnectionMode mode) {
        Optional<User> existingUser = userRepository.findByUsername(username);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            user.setConnectionMode(mode);
            user.setOnline(true);
            user.setLastSeen(LocalDateTime.now());
        } else {
            user = User.builder()
                    .username(username)
                    .connectionMode(mode)
                    .online(true)
                    .lastSeen(LocalDateTime.now())
                    .build();
        }

        user = userRepository.save(user);

        // Only announce if user is in PUBLIC mode
        if (mode == User.ConnectionMode.PUBLIC) {
            UserStatusDTO statusDTO = UserStatusDTO.fromEntity(user);
            kafkaTemplate.send(KafkaConfig.TOPIC_USER_STATUS, statusDTO);
            log.info("User connected: {}", username);
        } else {
            log.info("User connected in pseudo-anonymous mode");
        }

        return user;
    }

    @Transactional
    public void disconnectUser(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setOnline(false);
            user.setLastSeen(LocalDateTime.now());
            userRepository.save(user);

            // Only announce if user was in PUBLIC mode
            if (user.getConnectionMode() == User.ConnectionMode.PUBLIC) {
                UserStatusDTO statusDTO = UserStatusDTO.fromEntity(user);
                kafkaTemplate.send(KafkaConfig.TOPIC_USER_STATUS, statusDTO);
            }

            log.info("User disconnected: {}", username);
        }
    }

    public List<UserStatusDTO> getOnlineUsers() {
        return userRepository.findAllOnlinePublicUsers().stream()
                .map(UserStatusDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean isUserOnline(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        return userOpt.map(User::isOnline).orElse(false);
    }
}
