package org.insa.intergicielmessagerie.repository;

import org.insa.intergicielmessagerie.model.FileAttachment;
import org.insa.intergicielmessagerie.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {

    @Query("SELECT f FROM FileAttachment f WHERE f.expiryTime < :now")
    List<FileAttachment> findExpiredFiles(@Param("now") LocalDateTime now);

    //@Query("SELECT f FROM FileAttachment f JOIN f.message m WHERE m.receiver = :user AND f.downloaded = false")
    // just for the pending files, means the files that are not downloaded yet
    @Query("SELECT f FROM FileAttachment f JOIN f.message m WHERE m.receiver = :user ORDER BY f.id DESC") // all files for the user
    List<FileAttachment> findPendingFilesByUser(@Param("user") User user);

    Optional<FileAttachment> findByStoredFilename(String storedFilename);
}