package dev.thesis.janus.central.service;

import dev.thesis.janus.central.dto.NodeConnectionDTO;
import dev.thesis.janus.central.dto.NodeCreateDTO;
import dev.thesis.janus.central.dto.NodeDTO;
import dev.thesis.janus.central.exception.ResourceNotFoundException;
import dev.thesis.janus.central.model.Floor;
import dev.thesis.janus.central.model.Node;
import dev.thesis.janus.central.model.NodeConnection;
import dev.thesis.janus.central.repository.FloorRepository;
import dev.thesis.janus.central.repository.NodeConnectionRepository;
import dev.thesis.janus.central.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NodeService {

    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final FloorRepository floorRepository;

    @Transactional(readOnly = true)
    public List<NodeDTO> getAllNodes() {
        return nodeRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NodeDTO> getNodesByFloorId(Long floorId) {
        return nodeRepository.findByFloorId(floorId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NodeDTO> getElevationNodesByFloorId(Long floorId) {
        return nodeRepository.findByFloorIdAndIsElevationNodeTrue(floorId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NodeDTO> getNodesByBuildingId(Long buildingId) {
        return nodeRepository.findAllByBuildingId(buildingId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NodeDTO getNodeById(Long id) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Node not found with id: " + id));
        return convertToDTO(node);
    }

    @Transactional
    public NodeDTO createNode(NodeCreateDTO nodeCreateDTO) {
        Floor floor = floorRepository.findById(nodeCreateDTO.getFloorId())
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found with id: " + nodeCreateDTO.getFloorId()));

        Node node = new Node();
        node.setX(nodeCreateDTO.getX());
        node.setY(nodeCreateDTO.getY());
        node.setFloor(floor);
        node.setElevationNode(nodeCreateDTO.isElevationNode());
        node.setNodeType(nodeCreateDTO.getNodeType());

        Node savedNode = nodeRepository.save(node);
        return convertToDTO(savedNode);
    }

    @Transactional
    public NodeDTO updateNode(Long id, NodeCreateDTO nodeCreateDTO) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Node not found with id: " + id));

        Floor floor = floorRepository.findById(nodeCreateDTO.getFloorId())
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found with id: " + nodeCreateDTO.getFloorId()));

        node.setX(nodeCreateDTO.getX());
        node.setY(nodeCreateDTO.getY());
        node.setFloor(floor);
        node.setElevationNode(nodeCreateDTO.isElevationNode());
        node.setNodeType(nodeCreateDTO.getNodeType());

        Node updatedNode = nodeRepository.save(node);
        return convertToDTO(updatedNode);
    }

    @Transactional
    public void deleteNode(Long id) {
        if (!nodeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Node not found with id: " + id);
        }
        nodeRepository.deleteById(id);
    }
    @Transactional
    public void cleanupDuplicateNeighbors(Long buildingId) {
        List<Node> nodes = nodeRepository.findAllByBuildingId(buildingId);

        for (Node node : nodes) {
           

            Set<Node> uniqueNeighbors = new HashSet<>(node.getNeighbors());

           

            if (uniqueNeighbors.size() < node.getNeighbors().size()) {
                node.setNeighbors(uniqueNeighbors);
                nodeRepository.save(node);
            }
        }
    }

    @Transactional
    public NodeConnectionDTO createNodeConnection(NodeConnectionDTO connectionDTO) {
       

        Node sourceNode = nodeRepository.findById(connectionDTO.getSourceNodeId())
                .orElseThrow(() -> new ResourceNotFoundException("Source node not found"));
        Node targetNode = nodeRepository.findById(connectionDTO.getTargetNodeId())
                .orElseThrow(() -> new ResourceNotFoundException("Target node not found"));

       

        NodeConnection connection = new NodeConnection();
        connection.setSourceNode(sourceNode);
        connection.setTargetNode(targetNode);
        connection.setDistance(connectionDTO.getDistance());
        connection.setBidirectional(connectionDTO.isBidirectional());

        NodeConnection savedConnection = nodeConnectionRepository.save(connection);

        return new NodeConnectionDTO(
                savedConnection.getSourceNode().getId(),
                savedConnection.getTargetNode().getId(),
                savedConnection.getDistance(),
                savedConnection.isBidirectional()
        );
    }

    @Transactional
    public List<NodeConnectionDTO> getNodeConnectionsByBuildingId(Long buildingId) {
        return nodeConnectionRepository.findAllByBuildingId(buildingId).stream()
                .map(connection -> new NodeConnectionDTO(
                        connection.getSourceNode().getId(),
                        connection.getTargetNode().getId(),
                        connection.getDistance(),
                        connection.isBidirectional()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteNodeConnection(Long sourceNodeId, Long targetNodeId) {
        NodeConnection connection = nodeConnectionRepository.findBySourceNodeIdAndTargetNodeId(sourceNodeId, targetNodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found between nodes"));

       

        Node sourceNode = connection.getSourceNode();
        Node targetNode = connection.getTargetNode();

        sourceNode.getNeighbors().remove(targetNode);
        if (connection.isBidirectional()) {
            targetNode.getNeighbors().remove(sourceNode);
        }

        nodeRepository.save(sourceNode);
        if (connection.isBidirectional()) {
            nodeRepository.save(targetNode);
        }

        nodeConnectionRepository.delete(connection);
    }

    private NodeDTO convertToDTO(Node node) {
        Set<Long> neighborIds = node.getNeighbors().stream()
                .map(Node::getId)
                .collect(Collectors.toSet());

        return new NodeDTO(
                node.getId(),
                node.getX(),
                node.getY(),
                node.getFloor().getId(),
                node.isElevationNode(),
                node.getNodeType(),
                neighborIds,
                node.getCreatedAt(),
                node.getUpdatedAt()
        );
    }

    private float calculateDistance(Node sourceNode, Node targetNode) {
        float dx = sourceNode.getX() - targetNode.getX();
        float dy = sourceNode.getY() - targetNode.getY();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}