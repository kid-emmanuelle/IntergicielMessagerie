package org.insa.intergicielmessagerie.repository;

import org.insa.intergicielmessagerie.model.Message;
import org.insa.intergicielmessagerie.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE m.broadcast = true ORDER BY m.timestamp ASC")
    List<Message> findAllBroadcastMessages();

    @Query("SELECT m FROM Message m WHERE (m.sender = :user OR m.receiver = :user) ORDER BY m.timestamp ASC")
    List<Message> findUserMessages(@Param("user") User user);

    @Query("SELECT m FROM Message m WHERE (m.sender = :sender AND m.receiver = :receiver) OR (m.sender = :receiver AND m.receiver = :sender) ORDER BY m.timestamp ASC")
    List<Message> findMessagesBetweenUsers(@Param("sender") User sender, @Param("receiver") User receiver);
}