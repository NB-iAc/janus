package dev.thesis.janus.central.repository;

import dev.thesis.janus.central.model.UserBuildingPermissions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBuildingPermissionsRepository extends JpaRepository<UserBuildingPermissions, Long> {

    @Query("SELECT ubp FROM UserBuildingPermissions ubp WHERE ubp.user.userid = :userId")
    List<UserBuildingPermissions> findByUserId(Long userId);

    @Query("SELECT ubp FROM UserBuildingPermissions ubp " +
            "JOIN FETCH ubp.building " +
            "WHERE ubp.user.userid = :userId")
    List<UserBuildingPermissions> findByUserIdWithBuilding(Long userId);

    @Query("SELECT ubp FROM UserBuildingPermissions ubp WHERE ubp.user.userid = :userId AND ubp.building.id = :buildingId")
    Optional<UserBuildingPermissions> findByUserIdAndBuildingId(Long userId, Long buildingId);

    @Query("SELECT ubp FROM UserBuildingPermissions ubp " +
            "JOIN FETCH ubp.user " +
            "WHERE ubp.building.id = :buildingId AND ubp.allAccess = true")
    List<UserBuildingPermissions> findByBuildingIdWithAllAccess(Long buildingId);

}