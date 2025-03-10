package org.insa.intergicielmessagerie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.insa.intergicielmessagerie.model.User;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusDTO {

    private String username;
    private ConnectionMode connectionMode;
    private boolean online;

    public enum ConnectionMode {
        PUBLIC, PSEUDO_ANONYMOUS
    }

    public static UserStatusDTO fromEntity(User user) {
        return UserStatusDTO.builder()
                .username(user.getUsername())
                .connectionMode(user.getConnectionMode() == User.ConnectionMode.PUBLIC ?
                        ConnectionMode.PUBLIC : ConnectionMode.PSEUDO_ANONYMOUS)
                .online(user.isOnline())
                .build();
    }
}
