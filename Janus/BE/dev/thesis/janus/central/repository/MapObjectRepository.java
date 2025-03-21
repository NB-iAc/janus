package dev.thesis.janus.central.repository;

import dev.thesis.janus.central.model.Floor;
import dev.thesis.janus.central.model.MapObject;
import dev.thesis.janus.central.model.MapObjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MapObjectRepository extends JpaRepository<MapObject, Long> {
   

    List<MapObject> findByFloorIdAndObjectTypeIn(Long floorId, List<MapObjectType> objectTypes);
    List<MapObject> findByFloor(Floor floor);

    List<MapObject> findByFloorId(Long floorId);

    List<MapObject> findByFloorIdAndObjectType(Long floorId, MapObjectType objectType);

    Optional<MapObject> findByRoomId(String roomId);

    @Query("SELECT mo FROM MapObject mo WHERE mo.floor.building.id = :buildingId")
    List<MapObject> findAllByBuildingId(Long buildingId);
    @Query("SELECT mo FROM MapObject mo WHERE mo.floor.building.id IN :buildingIds AND mo.objectType = :objectType")
    List<MapObject> findAllByBuildingIdInAndObjectType(List<Long> buildingIds, MapObjectType objectType);

}
