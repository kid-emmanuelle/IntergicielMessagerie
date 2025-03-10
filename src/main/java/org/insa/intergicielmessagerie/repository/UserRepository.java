package org.insa.intergicielmessagerie.repository;

import org.insa.intergicielmessagerie.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.online = true AND u.connectionMode = 'PUBLIC'")
    List<User> findAllOnlinePublicUsers();
}