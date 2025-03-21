package dev.thesis.janus.central.repository;

import dev.thesis.janus.central.model.Floor;
import dev.thesis.janus.central.model.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NodeRepository extends JpaRepository<Node, Long> {

    List<Node> findByFloor(Floor floor);

    List<Node> findByFloorAndIsElevationNodeTrue(Floor floor);

    @Query("SELECT n FROM Node n WHERE n.floor.building.id = :buildingId")
    List<Node> findAllByBuildingId(Long buildingId);

    List<Node> findByFloorId(Long floorId);

    List<Node> findByFloorIdAndIsElevationNodeTrue(Long floorId);

    

    @Query("SELECT DISTINCT n FROM Node n " +
            "LEFT JOIN FETCH n.neighbors " +
            "WHERE n.floor.building.id = :buildingId")
    List<Node> findAllNodesByBuildingIdWithNeighbors(Long buildingId);

}