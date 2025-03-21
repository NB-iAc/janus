package dev.thesis.janus.central.repository;

import dev.thesis.janus.central.model.UserRequestList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRequestListRepository extends JpaRepository<UserRequestList, Long> {
    @Query("SELECT url FROM UserRequestList url WHERE url.requestingUser.userid = :userId")
    List<UserRequestList> findByUserId(Long userId);

    @Query("SELECT url FROM UserRequestList url WHERE url.building.id = :buildingId")
    List<UserRequestList> findByBuildingId(Long buildingId);

    @Query("SELECT url FROM UserRequestList url WHERE url.requestingUser.userid = :userId AND url.building.id = :buildingId AND url.roomId = :roomId")
    List<UserRequestList> findByUserIdAndBuildingIdAndRoomId(Long userId, Long buildingId, String roomId);

    @Query("SELECT url FROM UserRequestList url WHERE url.building.id = :buildingId AND url.status = :status")
    List<UserRequestList> findByBuildingIdAndStatus(Long buildingId, String status);
}