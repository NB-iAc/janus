package dev.thesis.janus.central.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.Query;

import dev.thesis.janus.central.model.Node;
import dev.thesis.janus.central.model.NodeConnection;

@Repository
public interface NodeConnectionRepository extends JpaRepository<NodeConnection, Long> {

    List<NodeConnection> findBySourceNode(Node sourceNode);

    List<NodeConnection> findByTargetNode(Node targetNode);

    Optional<NodeConnection> findBySourceNodeAndTargetNode(Node sourceNode, Node targetNode);

    @Query("SELECT nc FROM NodeConnection nc WHERE nc.sourceNode.id = :sourceNodeId AND nc.targetNode.id = :targetNodeId")
    Optional<NodeConnection> findBySourceNodeIdAndTargetNodeId(Long sourceNodeId, Long targetNodeId);

    @Query("SELECT nc FROM NodeConnection nc WHERE nc.sourceNode.floor.building.id = :buildingId OR nc.targetNode.floor.building.id = :buildingId")
    List<NodeConnection> findAllByBuildingId(Long buildingId);
}