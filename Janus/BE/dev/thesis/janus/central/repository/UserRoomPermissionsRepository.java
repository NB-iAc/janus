package dev.thesis.janus.central.repository;

import dev.thesis.janus.central.model.UserRoomPermissions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoomPermissionsRepository extends JpaRepository<UserRoomPermissions, Long> {

    @Query("SELECT urp FROM UserRoomPermissions urp WHERE urp.user.userid = :userId")
    List<UserRoomPermissions> findByUserId(Long userId);

    @Query("SELECT urp FROM UserRoomPermissions urp " +
            "JOIN FETCH urp.mapObject " +
            "JOIN FETCH urp.mapObject.floor " +
            "WHERE urp.user.userid = :userId")
    List<UserRoomPermissions> findByUserIdWithMapObject(Long userId);

    @Query("SELECT urp FROM UserRoomPermissions urp WHERE urp.user.userid = :userId AND urp.mapObject.id = :mapObjectId")
    Optional<UserRoomPermissions> findByUserIdAndMapObjectId(Long userId, Long mapObjectId);
}
