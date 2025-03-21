package dev.thesis.janus.central.repository;

import dev.thesis.janus.central.model.Building;
import dev.thesis.janus.central.model.Floor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FloorRepository extends JpaRepository<Floor, Long> {
    List<Floor> findByBuilding(Building building);

    List<Floor> findByBuildingId(Long buildingId);

    Optional<Floor> findByBuildingIdAndFloorNumber(Long buildingId, Integer floorNumber);
}
