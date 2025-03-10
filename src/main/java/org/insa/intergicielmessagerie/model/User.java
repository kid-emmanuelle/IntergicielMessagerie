package org.insa.intergicielmessagerie.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    private ConnectionMode connectionMode;

    private boolean online;

    private LocalDateTime lastSeen;

    @PrePersist
    protected void onCreate() {
        lastSeen = LocalDateTime.now();
    }

    public enum ConnectionMode {
        PUBLIC, PSEUDO_ANONYMOUS
    }
}