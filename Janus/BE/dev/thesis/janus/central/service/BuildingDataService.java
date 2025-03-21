package dev.thesis.janus.central.service;

import dev.thesis.janus.central.dto.BuildingCreateDTO;
import dev.thesis.janus.central.dto.BuildingDTO;
import dev.thesis.janus.central.exception.ResourceNotFoundException;
import dev.thesis.janus.central.model.Building;
import dev.thesis.janus.central.repository.BuildingRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.thesis.janus.central.dto.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;


import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.FlushMode;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import dev.thesis.janus.central.model.*;
import dev.thesis.janus.central.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class BuildingDataService {
   

    private static final Logger logger = LoggerFactory.getLogger(BuildingDataService.class);

    private final BuildingService buildingService;
    private final FloorService floorService;
    private final MapObjectService mapObjectService;
    private final NodeService nodeService;
    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final MapObjectRepository mapObjectRepository;
    private final MapObjectPointRepository mapObjectPointRepository;
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final EntityManagerFactory entityManagerFactory;

    

    @Transactional(readOnly = true)
    public BuildingDataDTO getBuildingData(Long buildingId) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting optimized data load for building ID: {}", buildingId);

       

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + buildingId));
        BuildingDTO buildingDTO = convertBuildingToDTO(building);

       

        List<Floor> floors = floorRepository.findByBuildingId(buildingId);
        List<FloorDTO> floorDTOs = floors.stream()
                .map(this::convertFloorToDTO)
                .collect(Collectors.toList());

       

        List<Long> floorIds = floors.stream()
                .map(Floor::getId)
                .collect(Collectors.toList());

       

        List<MapObject> mapObjects = mapObjectRepository.findAllByBuildingId(buildingId);

       

        List<Long> mapObjectIds = mapObjects.stream()
                .map(MapObject::getId)
                .collect(Collectors.toList());

        Map<Long, List<MapObjectPoint>> pointsByMapObjectId = new HashMap<>();
        if (!mapObjectIds.isEmpty()) {
            mapObjectPointRepository.findAllByMapObjectIdIn(mapObjectIds).forEach(point -> {
                pointsByMapObjectId
                        .computeIfAbsent(point.getMapObject().getId(), k ->
                                new java.util.ArrayList<>())
                        .add(point);
            });
        }

       

        Map<Long, List<MapObjectDTO>> mapObjectsByFloor = new HashMap<>();
        for (MapObject mapObject : mapObjects) {
            Long floorId = mapObject.getFloor().getId();
            List<MapObjectDTO> floorObjects = mapObjectsByFloor
                    .computeIfAbsent(floorId, k -> new java.util.ArrayList<>());

           

            List<MapObjectPoint> points = pointsByMapObjectId.getOrDefault(mapObject.getId(),
                    java.util.Collections.emptyList());

           

            points.sort((p1, p2) -> p1.getPointOrder().compareTo(p2.getPointOrder()));

            List<PointDTO> pointDTOs = points.stream()
                    .map(point -> new PointDTO(point.getX(), point.getY()))
                    .collect(Collectors.toList());

            floorObjects.add(convertMapObjectToDTO(mapObject, pointDTOs));
        }

       

        List<Node> nodes = nodeRepository.findAllNodesByBuildingIdWithNeighbors(buildingId);
        List<NodeDTO> nodeDTOs = nodes.stream()
                .map(this::convertNodeToDTO)
                .collect(Collectors.toList());

       

        List<NodeConnection> connections = nodeConnectionRepository.findAllByBuildingId(buildingId);
        List<NodeConnectionDTO> connectionDTOs = connections.stream()
                .map(this::convertConnectionToDTO)
                .collect(Collectors.toList());

        long endTime = System.currentTimeMillis();
        logger.info("Completed loading building data in {} ms", (endTime - startTime));

        return new BuildingDataDTO(buildingDTO, floorDTOs, mapObjectsByFloor, nodeDTOs, connectionDTOs);
    }
    private BuildingDTO convertBuildingToDTO(Building building) {
        return new BuildingDTO(
                building.getId(),
                building.getName(),
                building.getDescription(),
                building.getCreatedAt(),
                building.getUpdatedAt()
        );
    }

    private FloorDTO convertFloorToDTO(Floor floor) {
        return new FloorDTO(
                floor.getId(),
                floor.getBuilding().getId(),
                floor.getFloorNumber(),
                floor.getDisplayName(),
                floor.isAccessible()
        );
    }

    private MapObjectDTO convertMapObjectToDTO(MapObject mapObject, List<PointDTO> points) {
        return new MapObjectDTO(
                mapObject.getId(),
                mapObject.getFloor().getId(),
                mapObject.getObjectType(),
                mapObject.getName(),
                mapObject.getRoomId(),
                mapObject.getCategory(),
                mapObject.getContactDetails(),
                mapObject.getRoomType(),
                mapObject.getDescription(),
                mapObject.getEntranceNode() != null ? mapObject.getEntranceNode().getId() : null,
                points,
                mapObject.isAccessible(),
                mapObject.getCreatedAt(),
                mapObject.getUpdatedAt()
        );
    }

    private NodeDTO convertNodeToDTO(Node node) {
       

        if ("ELEVATION".equals(node.getNodeType())) {
           

            if (!node.isElevationNode()) {
                node.setElevationNode(true);
                nodeRepository.save(node);
            }
        }

        java.util.Set<Long> neighborIds = node.getNeighbors().stream()
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

    private NodeConnectionDTO convertConnectionToDTO(NodeConnection connection) {
        return new NodeConnectionDTO(
                connection.getSourceNode().getId(),
                connection.getTargetNode().getId(),
                connection.getDistance(),
                connection.isBidirectional()
        );
    }
    @Transactional
    public BuildingDataDTO uploadBuildingData(BuildingDataDTO buildingData) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting bulk upload process");
        logger.info("Input data summary - Floors: {}, Nodes: {}, Map Objects: {}, Connections: {}",
                buildingData.getFloors().size(),
                buildingData.getNodes().size(),
                buildingData.getMapObjectsByFloor().values().stream().mapToInt(List::size).sum(),
                buildingData.getConnections().size());

        try {
           

            Map<Long, Long> buildingIdMapping = new HashMap<>();
            Map<Long, Long> floorIdMapping = new HashMap<>();
            Map<Long, Long> nodeIdMapping = new HashMap<>();
            Map<Long, Long> mapObjectIdMapping = new HashMap<>();

           

            BuildingDTO building = processBuilding(buildingData.getBuilding(), buildingIdMapping);
            logger.info("Building processed - ID: {}, Name: {}", building.getId(), building.getName());

           

            logger.info("Starting floor processing");
            buildingData.getFloors().forEach(floorDTO -> {
                try {
                    FloorDTO processedFloor = processFloor(floorDTO, buildingIdMapping, floorIdMapping);
                    logger.debug("Processed floor - Original ID: {}, New ID: {}, Number: {}",
                            floorDTO.getId(), processedFloor.getId(), processedFloor.getFloorNumber());
                } catch (Exception e) {
                    logger.error("Failed to process floor: {}", floorDTO, e);
                    throw new RuntimeException("Floor processing failed", e);
                }
            });

           

            logger.info("Starting node processing");
            final int NODE_BATCH_SIZE = 100;
            List<NodeDTO> nodes = buildingData.getNodes();
            for (int i = 0; i < nodes.size(); i += NODE_BATCH_SIZE) {
                int endIndex = Math.min(i + NODE_BATCH_SIZE, nodes.size());
                List<NodeDTO> batch = nodes.subList(i, endIndex);

                logger.info("Processing node batch {} - {} to {}",
                        (i / NODE_BATCH_SIZE) + 1, i, endIndex);

                batch.forEach(nodeDTO -> {
                    try {
                        NodeDTO processedNode = processNode(nodeDTO, floorIdMapping, nodeIdMapping);
                        logger.debug("Processed node - Original ID: {}, New ID: {}",
                                nodeDTO.getId(), processedNode.getId());
                    } catch (Exception e) {
                        logger.error("Failed to process node: {}", nodeDTO, e);
                        throw new RuntimeException("Node processing failed", e);
                    }
                });
            }

           

            logger.info("Starting map object processing");
            buildingData.getMapObjectsByFloor().forEach((floorId, mapObjects) -> {
                Long mappedFloorId = mapId(floorId, floorIdMapping);
                logger.info("Processing map objects for floor - Original ID: {}, Mapped ID: {}",
                        floorId, mappedFloorId);

                final int MAP_OBJECT_BATCH_SIZE = 50;
                for (int i = 0; i < mapObjects.size(); i += MAP_OBJECT_BATCH_SIZE) {
                    int endIndex = Math.min(i + MAP_OBJECT_BATCH_SIZE, mapObjects.size());
                    List<MapObjectDTO> batch = mapObjects.subList(i, endIndex);

                    logger.info("Processing map object batch {} - {} to {}",
                            (i / MAP_OBJECT_BATCH_SIZE) + 1, i, endIndex);

                    batch.forEach(mapObjectDTO -> {
                        try {
                            MapObjectDTO processedMapObject = processMapObject(
                                    mapObjectDTO,
                                    mappedFloorId,
                                    nodeIdMapping,
                                    mapObjectIdMapping
                            );
                            logger.debug("Processed map object - Original ID: {}, New ID: {}",
                                    mapObjectDTO.getId(), processedMapObject.getId());
                        } catch (Exception e) {
                            logger.error("Failed to process map object: {}", mapObjectDTO, e);
                            throw new RuntimeException("Map object processing failed", e);
                        }
                    });
                }
            });
            logger.info("Fetching existing node connections for building ID: {}", building.getId());
            List<NodeConnection> existingConnections = nodeConnectionRepository.findAllByBuildingId(building.getId());

            Map<String, NodeConnection> existingConnectionMap = new HashMap<>();
            for (NodeConnection conn : existingConnections) {
                String forwardKey = conn.getSourceNode().getId() + "-" + conn.getTargetNode().getId();
                existingConnectionMap.put(forwardKey, conn);

                if (conn.isBidirectional()) {
                    String reverseKey = conn.getTargetNode().getId() + "-" + conn.getSourceNode().getId();
                    existingConnectionMap.put(reverseKey, conn);
                }
            }
            logger.info("Starting node connection processing with duplicate prevention");
            final int CONNECTION_BATCH_SIZE = 200;
            List<NodeConnectionDTO> connections = buildingData.getConnections();

            List<NodeConnectionDTO> mappedConnections = connections.stream()
                    .map(conn -> new NodeConnectionDTO(
                            mapId(conn.getSourceNodeId(), nodeIdMapping),
                            mapId(conn.getTargetNodeId(), nodeIdMapping),
                            conn.getDistance(),
                            conn.isBidirectional()
                    ))
                    .filter(conn -> conn.getSourceNodeId() != null && conn.getTargetNodeId() != null)
                    .collect(Collectors.toList());

            for (int i = 0; i < mappedConnections.size(); i += CONNECTION_BATCH_SIZE) {
                int endIndex = Math.min(i + CONNECTION_BATCH_SIZE, mappedConnections.size());
                List<NodeConnectionDTO> batch = mappedConnections.subList(i, endIndex);

                logger.info("Processing connection batch {} - {} to {}",
                        (i / CONNECTION_BATCH_SIZE) + 1, i, endIndex);

                processBatchNodeConnectionsWithDuplicatePrevention(batch, existingConnectionMap);
            }

           

            long endTime = System.currentTimeMillis();
            logger.info("Completed bulk upload in {} ms", (endTime - startTime));

            Long buildingId = building.getId();
            nodeService.cleanupDuplicateNeighbors(buildingId);
            return getBuildingData(building.getId());
        } catch (Exception e) {
            logger.error("Comprehensive upload failure", e);
            throw new RuntimeException("Failed to upload building data completely", e);
        }
    }
    private void processBatchNodeConnectionsWithDuplicatePrevention(
            List<NodeConnectionDTO> connections,
            Map<String, NodeConnection> existingConnectionMap) {

        logger.info("Processing {} node connections with duplicate prevention", connections.size());
        int skippedCount = 0;
        int updatedCount = 0;
        int createdCount = 0;
        int errorCount = 0;

        for (NodeConnectionDTO connectionDTO : connections) {
            try {
                Long sourceNodeId = connectionDTO.getSourceNodeId();
                Long targetNodeId = connectionDTO.getTargetNodeId();
                String connectionKey = sourceNodeId + "-" + targetNodeId;

               

                NodeConnection existingConnection = existingConnectionMap.get(connectionKey);

                if (existingConnection != null) {
                   

                    if (Math.abs(existingConnection.getDistance() - connectionDTO.getDistance()) > 0.001f) {
                       

                        existingConnection.setDistance(connectionDTO.getDistance());
                        nodeConnectionRepository.save(existingConnection);
                        updatedCount++;
                        logger.debug("Updated existing connection: {}", connectionKey);
                    } else {
                       

                        skippedCount++;
                        logger.debug("Skipped duplicate connection: {}", connectionKey);
                    }
                } else {
                   

                    nodeService.createNodeConnection(connectionDTO);

                   

                    String forwardKey = sourceNodeId + "-" + targetNodeId;

                   

                    NodeConnection newConn = new NodeConnection();
                    Node sourceNode = new Node();
                    sourceNode.setId(sourceNodeId);
                    Node targetNode = new Node();
                    targetNode.setId(targetNodeId);
                    newConn.setSourceNode(sourceNode);
                    newConn.setTargetNode(targetNode);
                    newConn.setDistance(connectionDTO.getDistance());
                    newConn.setBidirectional(connectionDTO.isBidirectional());

                    existingConnectionMap.put(forwardKey, newConn);

                   

                    if (connectionDTO.isBidirectional()) {
                        String reverseKey = targetNodeId + "-" + sourceNodeId;
                        existingConnectionMap.put(reverseKey, newConn);
                    }

                    createdCount++;
                    logger.debug("Created new connection: {}", connectionKey);
                }
            } catch (Exception e) {
                errorCount++;
                logger.error("Failed to process node connection: {}", connectionDTO, e);
            }
        }

        logger.info("Node connection batch processing summary - Created: {}, Updated: {}, Skipped: {}, Errors: {}",
                createdCount, updatedCount, skippedCount, errorCount);
    }
    private void processBatchNodeConnections(List<NodeConnectionDTO> connections) {
        logger.info("Processing {} node connections", connections.size());

        int successCount = 0;
        int errorCount = 0;

        for (NodeConnectionDTO connection : connections) {
            try {
               

                if (connection.getSourceNodeId() == null || connection.getTargetNodeId() == null) {
                    logger.warn("Skipping connection with null node IDs: {}", connection);
                    errorCount++;
                    continue;
                }

                nodeService.createNodeConnection(connection);
                successCount++;
                logger.debug("Successfully created connection: {}", connection);
            } catch (Exception e) {
                errorCount++;
                logger.error("Failed to create node connection: {}", connection, e);
            }
        }

        logger.info("Node connection batch processing summary - Success: {}, Errors: {}",
                successCount, errorCount);
    }



    

    private void optimizeEntityManagerForBatch() {
        try {
           

            EntityManager em = ((LocalContainerEntityManagerFactoryBean) entityManagerFactory)
                    .getNativeEntityManagerFactory().createEntityManager();

            Session session = em.unwrap(Session.class);
            session.setJdbcBatchSize(50);

            session.setHibernateFlushMode(FlushMode.COMMIT);

        } catch (Exception e) {
            logger.debug("Could not optimize entity manager for batch operations", e);
        }
    }

    private BuildingDTO processBuilding(BuildingDTO buildingDTO, Map<Long, Long> buildingIdMapping) {
        BuildingCreateDTO buildingCreateDTO = new BuildingCreateDTO(
                buildingDTO.getName(),
                buildingDTO.getDescription()
        );

        Long originalId = buildingDTO.getId();
        BuildingDTO result;

        if (originalId == null) {
           

            result = buildingService.createBuilding(buildingCreateDTO);
        } else if (originalId > 0) {
           

            try {
                result = buildingService.updateBuilding(originalId, buildingCreateDTO);
            } catch (ResourceNotFoundException e) {
                result = buildingService.createBuilding(buildingCreateDTO);
                buildingIdMapping.put(originalId, result.getId());
            }
        } else {
           

            result = buildingService.createBuilding(buildingCreateDTO);
            buildingIdMapping.put(originalId, result.getId());
        }

        return result;
    }

    private FloorDTO processFloor(FloorDTO floorDTO, Map<Long, Long> buildingIdMapping, Map<Long, Long> floorIdMapping) {
        Long originalId = floorDTO.getId();
        Long mappedBuildingId = mapId(floorDTO.getBuildingId(), buildingIdMapping);

        FloorCreateDTO floorCreateDTO = new FloorCreateDTO(
                mappedBuildingId,
                floorDTO.getFloorNumber(),
                floorDTO.getDisplayName(),
                floorDTO.isAccessible()
        );

        FloorDTO result;

        if (originalId == null) {
           

            result = floorService.createFloor(floorCreateDTO);
        } else if (originalId > 0) {
           

            try {
                result = floorService.updateFloor(originalId, floorCreateDTO);
            } catch (ResourceNotFoundException e) {
                result = floorService.createFloor(floorCreateDTO);
                floorIdMapping.put(originalId, result.getId());
            }
        } else {
           

            result = floorService.createFloor(floorCreateDTO);
            floorIdMapping.put(originalId, result.getId());
        }

        return result;
    }

    private NodeDTO processNode(NodeDTO nodeDTO, Map<Long, Long> floorIdMapping, Map<Long, Long> nodeIdMapping) {
        Long originalId = nodeDTO.getId();
        Long mappedFloorId = mapId(nodeDTO.getFloorId(), floorIdMapping);

        NodeCreateDTO nodeCreateDTO = new NodeCreateDTO(
                nodeDTO.getX(),
                nodeDTO.getY(),
                mappedFloorId,
                nodeDTO.isElevationNode(),
                nodeDTO.getNodeType()
        );

        NodeDTO result;

        if (originalId == null) {
           

            result = nodeService.createNode(nodeCreateDTO);
        } else if (originalId > 0) {
           

            try {
                result = nodeService.updateNode(originalId, nodeCreateDTO);
            } catch (ResourceNotFoundException e) {
                result = nodeService.createNode(nodeCreateDTO);
                nodeIdMapping.put(originalId, result.getId());
            }
        } else {
           

            result = nodeService.createNode(nodeCreateDTO);
            nodeIdMapping.put(originalId, result.getId());
        }

        return result;
    }

    private MapObjectDTO processMapObject(MapObjectDTO mapObjectDTO, Long floorId, Map<Long, Long> nodeIdMapping, Map<Long, Long> mapObjectIdMapping) {
        Long originalId = mapObjectDTO.getId();
        Long mappedEntranceNodeId = mapId(mapObjectDTO.getEntranceNodeId(), nodeIdMapping);

        MapObjectCreateDTO createDTO = new MapObjectCreateDTO(
                floorId,
                mapObjectDTO.getObjectType(),
                mapObjectDTO.getName(),
                mapObjectDTO.getRoomId(),
                mapObjectDTO.getCategory(),
                mapObjectDTO.getContactDetails(),
                mapObjectDTO.getRoomType(),
                mapObjectDTO.getDescription(),
                mappedEntranceNodeId,
                mapObjectDTO.getPoints(),
                mapObjectDTO.isAccessible()
        );

        MapObjectDTO result;

        if (originalId == null) {
           

            result = mapObjectService.createMapObject(createDTO);
        } else if (originalId > 0) {
           

            try {
                result = mapObjectService.updateMapObject(originalId, createDTO);
            } catch (Exception e) {
                result = mapObjectService.createMapObject(createDTO);
                mapObjectIdMapping.put(originalId, result.getId());
            }
        } else {
           

            result = mapObjectService.createMapObject(createDTO);
            mapObjectIdMapping.put(originalId, result.getId());
        }

        return result;
    }

    private void processNodeConnection(NodeConnectionDTO connectionDTO, Map<Long, Long> nodeIdMapping) {
        Long mappedSourceNodeId = mapId(connectionDTO.getSourceNodeId(), nodeIdMapping);
        Long mappedTargetNodeId = mapId(connectionDTO.getTargetNodeId(), nodeIdMapping);

        NodeConnectionDTO mappedConnectionDTO = new NodeConnectionDTO(
                mappedSourceNodeId,
                mappedTargetNodeId,
                connectionDTO.getDistance(),
                connectionDTO.isBidirectional()
        );

        try {
            nodeService.createNodeConnection(mappedConnectionDTO);
        } catch (Exception e) {
           

            logger.debug("Duplicate connection or error creating connection", e);
        }
    }
    private Long mapId(Long originalId, Map<Long, Long> idMapping) {
        if (originalId == null) {
            return null;
        }

        if (originalId < 0 && idMapping.containsKey(originalId)) {
            return idMapping.get(originalId);
        }

        return originalId;
    }
}