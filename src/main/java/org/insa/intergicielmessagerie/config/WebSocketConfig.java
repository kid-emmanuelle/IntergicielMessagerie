package org.insa.intergicielmessagerie.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
                        // Lấy username từ parameters trong URL handshake
                        String username = null;
                        if (request.getURI().getQuery() != null) {
                            String query = request.getURI().getQuery();
                            for (String param : query.split("&")) {
                                String[] pair = param.split("=");
                                if (pair.length == 2 && "username".equals(pair[0])) {
                                    username = pair[1];
                                    attributes.put("username", username);
                                    break;
                                }
                            }
                        }

                        if (username == null) {
                            return null;
                        }
                        return new StompPrincipal(username);
                    }
                })
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix for messages that are bound for the broker (and then to subscribed clients)
        /* Use /topic for broadcast messages & use /queue for user-specific messages */
        registry.enableSimpleBroker("/topic", "/queue");
        // Prefix for messages that are bound for methods annotated with @MessageMapping
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
}

