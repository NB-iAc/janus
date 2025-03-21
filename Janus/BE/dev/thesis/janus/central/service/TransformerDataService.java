package dev.thesis.janus.central.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thesis.janus.central.dto.*;
import dev.thesis.janus.central.model.*;
import dev.thesis.janus.central.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransformerDataService {
    private static final Logger logger = LoggerFactory.getLogger(TransformerDataService.class);

    private final BuildingService buildingService;
    private final FloorService floorService;
    private final MapObjectService mapObjectService;
    private final NodeService nodeService;
    private final BuildingDataService buildingDataService;

    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final NodeRepository nodeRepository;
    private final MapObjectRepository mapObjectRepository;
    private final NodeConnectionRepository nodeConnectionRepository;

    

    @Transactional
    public BuildingDataDTO processTransformerData(JsonNode transformerData) {
        try {
            logger.info("Starting processing of transformer data");

           

            Map<String, Long> tempNodeIdToRealId = new HashMap<>();
            Map<String, Long> tempObjIdToRealId = new HashMap<>();
            Map<Integer, Long> floorNumberToId = new HashMap<>();

           

            BuildingDTO buildingDTO = processBuildingData(transformerData);
            logger.info("Processed building data: id={}, name={}", buildingDTO.getId(), buildingDTO.getName());

           

            List<FloorDTO> floorDTOs = processFloors(transformerData, buildingDTO.getId(), floorNumberToId);
            logger.info("Processed {} floors", floorDTOs.size());

           

            List<NodeDTO> nodeDTOs = processNodes(transformerData, floorNumberToId, tempNodeIdToRealId);
            logger.info("Processed {} nodes with {} temporary ID mappings",
                    nodeDTOs.size(), tempNodeIdToRealId.size());

           

            Map<Long, List<MapObjectDTO>> mapObjectsByFloor =
                    processMapObjects(transformerData, floorNumberToId, tempNodeIdToRealId, tempObjIdToRealId);
            logger.info("Processed map objects for {} floors", mapObjectsByFloor.size());

           

            List<NodeConnectionDTO> connectionDTOs =
                    processConnections(transformerData, tempNodeIdToRealId);
            logger.info("Processed {} connections", connectionDTOs.size());

           

            BuildingDataDTO result = new BuildingDataDTO(buildingDTO, floorDTOs, mapObjectsByFloor, nodeDTOs, connectionDTOs);
            logger.info("Successfully completed transformer data processing");
            return result;

        } catch (Exception e) {
            logger.error("Error processing transformer data", e);
            throw new RuntimeException("Failed to process transformer data", e);
        }
    }

    

    private BuildingDTO processBuildingData(JsonNode transformerData) {
        try {
            JsonNode buildingNode = transformerData.get("building_data");
            if (buildingNode == null) {
                throw new IllegalArgumentException("Building data not found in transformer data");
            }

            Long buildingId = null;
            if (buildingNode.has("id") && !buildingNode.get("id").isTextual()) {
                buildingId = buildingNode.get("id").asLong();
            }

            String name = buildingNode.get("name").asText();
            String description = buildingNode.has("description") ?
                    buildingNode.get("description").asText() : "";

            BuildingCreateDTO buildingCreateDTO = new BuildingCreateDTO(name, description);

            BuildingDTO buildingDTO;
            if (buildingId != null) {
                try {
                    buildingDTO = buildingService.updateBuilding(buildingId, buildingCreateDTO);
                    logger.info("Updated existing building with ID: {}", buildingId);
                } catch (Exception e) {
                    logger.warn("Could not update building with ID: {}. Creating new building.", buildingId);
                    buildingDTO = buildingService.createBuilding(buildingCreateDTO);
                    logger.info("Created new building with ID: {}", buildingDTO.getId());
                }
            } else {
                buildingDTO = buildingService.createBuilding(buildingCreateDTO);
                logger.info("Created new building with ID: {}", buildingDTO.getId());
            }

            return buildingDTO;
        } catch (Exception e) {
            logger.error("Error processing building data", e);
            throw new RuntimeException("Failed to process building data", e);
        }
    }

    

    private List<FloorDTO> processFloors(JsonNode transformerData, Long buildingId,
                                         Map<Integer, Long> floorNumberToId) {
        try {
            JsonNode floorNamesNode = transformerData.get("floorNames");
            if (floorNamesNode == null || !floorNamesNode.isObject()) {
                logger.warn("No floor names found in transformer data");
                return Collections.emptyList();
            }

            List<FloorDTO> floorDTOs = new ArrayList<>();

           

            List<Floor> existingFloors = floorRepository.findByBuildingId(buildingId);
            Map<Integer, Long> existingFloorNumberToId = existingFloors.stream()
                    .collect(Collectors.toMap(Floor::getFloorNumber, Floor::getId));

           

            Iterator<Map.Entry<String, JsonNode>> fields = floorNamesNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                Integer floorNumber = Integer.parseInt(entry.getKey());
                String displayName = entry.getValue().asText();

                FloorCreateDTO floorCreateDTO = new FloorCreateDTO(
                        buildingId,
                        floorNumber,
                        displayName,
                        true

                );

                FloorDTO floorDTO;
                if (existingFloorNumberToId.containsKey(floorNumber)) {
                    Long existingFloorId = existingFloorNumberToId.get(floorNumber);
                    floorDTO = floorService.updateFloor(existingFloorId, floorCreateDTO);
                    logger.info("Updated existing floor: number={}, id={}", floorNumber, existingFloorId);
                    existingFloorNumberToId.remove(floorNumber);

                } else {
                    floorDTO = floorService.createFloor(floorCreateDTO);
                    logger.info("Created new floor: number={}, id={}", floorNumber, floorDTO.getId());
                }

                floorNumberToId.put(floorNumber, floorDTO.getId());
                floorDTOs.add(floorDTO);
            }

           

            if (!existingFloorNumberToId.isEmpty()) {
                logger.info("Found {} floors to remove", existingFloorNumberToId.size());
                for (Long floorIdToRemove : existingFloorNumberToId.values()) {
                    try {
                        floorService.deleteFloor(floorIdToRemove);
                        logger.info("Deleted floor with ID: {}", floorIdToRemove);
                    } catch (Exception e) {
                        logger.error("Failed to delete floor with ID: {}", floorIdToRemove, e);
                    }
                }
            }

            return floorDTOs;
        } catch (Exception e) {
            logger.error("Error processing floors", e);
            throw new RuntimeException("Failed to process floors", e);
        }
    }

    

    private List<NodeDTO> processNodes(JsonNode transformerData, Map<Integer, Long> floorNumberToId,
                                       Map<String, Long> tempNodeIdToRealId) {
        try {
            JsonNode connectionPointsNode = transformerData.get("connection_points");
            if (connectionPointsNode == null || !connectionPointsNode.isArray()) {
                logger.warn("No connection points found in transformer data");
                return Collections.emptyList();
            }

            List<NodeDTO> nodeDTOs = new ArrayList<>();

           

            Map<Long, Set<Long>> existingNodesByFloor = new HashMap<>();
            floorNumberToId.values().forEach(floorId -> {
                List<Node> existingNodes = nodeRepository.findByFloorId(floorId);
                existingNodesByFloor.put(
                        floorId,
                        existingNodes.stream().map(Node::getId).collect(Collectors.toSet())
                );
            });

           

            for (JsonNode nodeNode : connectionPointsNode) {
                String nodeIdStr = nodeNode.get("id").asText();
                boolean isTemporaryId = nodeIdStr.startsWith("TEMP");
                Long nodeId = isTemporaryId ? null : Long.parseLong(nodeIdStr);

                Float x = (float) nodeNode.get("x").asDouble();
                Float y = (float) nodeNode.get("y").asDouble();
                Integer floorNumber = nodeNode.get("floor").asInt();
                boolean isElevation = nodeNode.has("isElevation") && nodeNode.get("isElevation").asBoolean();
                String nodeType = isElevation ? "ELEVATION" : "";

               

                if (!floorNumberToId.containsKey(floorNumber)) {
                    logger.error("Floor number {} not found in floor mapping", floorNumber);
                    continue;
                }
                Long floorId = floorNumberToId.get(floorNumber);

                NodeCreateDTO nodeCreateDTO = new NodeCreateDTO(
                        x, y, floorId, isElevation, nodeType
                );

                NodeDTO nodeDTO;
                if (nodeId != null) {
                   

                    try {
                        nodeDTO = nodeService.updateNode(nodeId, nodeCreateDTO);
                        logger.info("Updated existing node: id={}", nodeId);

                       

                        existingNodesByFloor.get(floorId).remove(nodeId);

                    } catch (Exception e) {
                        logger.warn("Could not update node with ID: {}. Creating new node.", nodeId);
                        nodeDTO = nodeService.createNode(nodeCreateDTO);
                        logger.info("Created new node with ID: {} (replacing {})", nodeDTO.getId(), nodeId);
                    }
                } else {
                   

                    nodeDTO = nodeService.createNode(nodeCreateDTO);
                    logger.info("Created new node: tempId={}, newId={}", nodeIdStr, nodeDTO.getId());

                   

                    tempNodeIdToRealId.put(nodeIdStr, nodeDTO.getId());
                }

                nodeDTOs.add(nodeDTO);
            }

           

            existingNodesByFloor.forEach((floorId, nodeIds) -> {
                if (!nodeIds.isEmpty()) {
                    logger.info("Found {} nodes to remove on floor {}", nodeIds.size(), floorId);
                    for (Long nodeIdToRemove : nodeIds) {
                        try {
                            nodeService.deleteNode(nodeIdToRemove);
                            logger.info("Deleted node with ID: {}", nodeIdToRemove);
                        } catch (Exception e) {
                            logger.error("Failed to delete node with ID: {}", nodeIdToRemove, e);
                        }
                    }
                }
            });

            return nodeDTOs;
        } catch (Exception e) {
            logger.error("Error processing nodes", e);
            throw new RuntimeException("Failed to process nodes", e);
        }
    }

    

    private Map<Long, List<MapObjectDTO>> processMapObjects(JsonNode transformerData,
                                                            Map<Integer, Long> floorNumberToId,
                                                            Map<String, Long> tempNodeIdToRealId,
                                                            Map<String, Long> tempObjIdToRealId) {
        try {
            JsonNode floorDrawingsNode = transformerData.get("floorDrawings");
            if (floorDrawingsNode == null || !floorDrawingsNode.isObject()) {
                logger.warn("No floor drawings found in transformer data");
                return Collections.emptyMap();
            }

            Map<Long, List<MapObjectDTO>> mapObjectsByFloor = new HashMap<>();

           

            Map<Long, Set<Long>> existingMapObjectsByFloor = new HashMap<>();
            floorNumberToId.values().forEach(floorId -> {
                List<MapObject> existingMapObjects = mapObjectRepository.findByFloorId(floorId);
                existingMapObjectsByFloor.put(
                        floorId,
                        existingMapObjects.stream().map(MapObject::getId).collect(Collectors.toSet())
                );
            });

           

            Iterator<Map.Entry<String, JsonNode>> floors = floorDrawingsNode.fields();
            while (floors.hasNext()) {
                Map.Entry<String, JsonNode> floorEntry = floors.next();
                Integer floorNumber = Integer.parseInt(floorEntry.getKey());

                if (!floorNumberToId.containsKey(floorNumber)) {
                    logger.error("Floor number {} not found in floor mapping", floorNumber);
                    continue;
                }
                Long floorId = floorNumberToId.get(floorNumber);
                List<MapObjectDTO> floorObjects = new ArrayList<>();

               

                for (JsonNode objNode : floorEntry.getValue()) {
                    String objIdStr = objNode.get("id").asText();
                    boolean isTemporaryId = objIdStr.startsWith("TEMPOBJ");
                    Long objId = isTemporaryId ? null : Long.parseLong(objIdStr);

                   

                    List<PointDTO> points = new ArrayList<>();
                    if (objNode.has("points") && objNode.get("points").isArray()) {
                        for (JsonNode pointNode : objNode.get("points")) {
                            points.add(new PointDTO(
                                    (float) pointNode.get("x").asDouble(),
                                    (float) pointNode.get("y").asDouble()
                            ));
                        }
                    } else if (objNode.has("linePointList") && objNode.get("linePointList").isArray()) {
                       

                        for (JsonNode pointNode : objNode.get("linePointList")) {
                            points.add(new PointDTO(
                                    (float) pointNode.get("x").asDouble(),
                                    (float) pointNode.get("y").asDouble()
                            ));
                        }
                    }

                    if (points.isEmpty()) {
                        logger.warn("No points found for object: {}", objIdStr);
                        continue;
                    }

                   

                    String typeStr = objNode.has("type") ? objNode.get("type").asText() : "ROOM";
                    MapObjectType objectType;
                    switch (typeStr.toUpperCase()) {
                        case "LINE":
                            objectType = MapObjectType.WALL;
                            break;
                        case "STAIR":
                            objectType = MapObjectType.STAIR;
                            break;
                        case "ELEVATOR":
                            objectType = MapObjectType.ELEVATOR;
                            break;
                        case "ESCALATOR":
                            objectType = MapObjectType.ESCALATOR;
                            break;
                        default:
                            objectType = MapObjectType.ROOM;
                    }

                   

                    JsonNode metadataNode = objNode.get("metadata");
                    String name = metadataNode != null && metadataNode.has("roomName") ?
                            metadataNode.get("roomName").asText() : "";
                    String category = metadataNode != null && metadataNode.has("roomCategory") ?
                            metadataNode.get("roomCategory").asText() : "DEFAULT";
                    String roomType = metadataNode != null && metadataNode.has("category") ?
                            metadataNode.get("category").asText() : "";
                    String description = metadataNode != null && metadataNode.has("description") ?
                            metadataNode.get("description").asText() : "";

                   

                    StringBuilder contactDetails = new StringBuilder();
                    if (metadataNode != null) {
                        if (metadataNode.has("owner")) {
                            contactDetails.append("Owner: ").append(metadataNode.get("owner").asText()).append("\n");
                        }
                        if (metadataNode.has("email")) {
                            contactDetails.append("Email: ").append(metadataNode.get("email").asText()).append("\n");
                        }
                        if (metadataNode.has("phone")) {
                            contactDetails.append("Phone: ").append(metadataNode.get("phone").asText());
                        }
                    }

                   

                    Long entranceNodeId = null;
                    if (objNode.has("entranceNode")) {
                        JsonNode entranceNode = objNode.get("entranceNode");
                        String entranceNodeIdStr = entranceNode.get("id").asText();

                        if (entranceNodeIdStr.startsWith("TEMP")) {
                           

                            if (tempNodeIdToRealId.containsKey(entranceNodeIdStr)) {
                                entranceNodeId = tempNodeIdToRealId.get(entranceNodeIdStr);
                            } else {
                                logger.warn("Temporary entrance node ID not found in mapping: {}", entranceNodeIdStr);
                            }
                        } else {
                           

                            entranceNodeId = Long.parseLong(entranceNodeIdStr);
                        }
                    }

                   

                    MapObjectCreateDTO mapObjectCreateDTO = new MapObjectCreateDTO(
                            floorId,
                            objectType,
                            name,
                            "",

                            category,
                            contactDetails.toString(),
                            roomType,
                            description,
                            entranceNodeId,
                            points,
                            true

                    );

                    MapObjectDTO mapObjectDTO;
                    if (objId != null) {
                       

                        try {
                            mapObjectDTO = mapObjectService.updateMapObject(objId, mapObjectCreateDTO);
                            logger.info("Updated existing map object: id={}", objId);

                           

                            existingMapObjectsByFloor.get(floorId).remove(objId);

                        } catch (Exception e) {
                            logger.warn("Could not update map object with ID: {}. Creating new object.", objId);
                            mapObjectDTO = mapObjectService.createMapObject(mapObjectCreateDTO);

                           

                            if (objectType == MapObjectType.ROOM) {
                                mapObjectDTO = mapObjectService.updateMapObject(mapObjectDTO.getId(),
                                        new MapObjectCreateDTO(
                                                floorId, objectType, name, "R-" + mapObjectDTO.getId(),
                                                category, contactDetails.toString(), roomType, description,
                                                entranceNodeId, points, true
                                        ));
                            }

                            logger.info("Created new map object with ID: {} (replacing {})", mapObjectDTO.getId(), objId);
                        }
                    } else {
                       

                        mapObjectDTO = mapObjectService.createMapObject(mapObjectCreateDTO);

                       

                        if (objectType == MapObjectType.ROOM) {
                            mapObjectDTO = mapObjectService.updateMapObject(mapObjectDTO.getId(),
                                    new MapObjectCreateDTO(
                                            floorId, objectType, name, "R-" + mapObjectDTO.getId(),
                                            category, contactDetails.toString(), roomType, description,
                                            entranceNodeId, points, true
                                    ));
                        }

                        logger.info("Created new map object: tempId={}, newId={}", objIdStr, mapObjectDTO.getId());

                       

                        tempObjIdToRealId.put(objIdStr, mapObjectDTO.getId());
                    }

                    floorObjects.add(mapObjectDTO);
                }

                mapObjectsByFloor.put(floorId, floorObjects);
            }

           

            existingMapObjectsByFloor.forEach((floorId, objIds) -> {
                if (!objIds.isEmpty()) {
                    logger.info("Found {} map objects to remove on floor {}", objIds.size(), floorId);
                    for (Long objIdToRemove : objIds) {
                        try {
                            mapObjectService.deleteMapObject(objIdToRemove);
                            logger.info("Deleted map object with ID: {}", objIdToRemove);
                        } catch (Exception e) {
                            logger.error("Failed to delete map object with ID: {}", objIdToRemove, e);
                        }
                    }
                }
            });

            return mapObjectsByFloor;
        } catch (Exception e) {
            logger.error("Error processing map objects", e);
            throw new RuntimeException("Failed to process map objects", e);
        }
    }

    

    private List<NodeConnectionDTO> processConnections(JsonNode transformerData,
                                                       Map<String, Long> tempNodeIdToRealId) {
        try {
            JsonNode connectionsNode = transformerData.get("connections");
            if (connectionsNode == null || !connectionsNode.isArray()) {
                logger.warn("No connections found in transformer data");
                return Collections.emptyList();
            }

           

           


            List<NodeConnectionDTO> connectionDTOs = new ArrayList<>();

            for (JsonNode connNode : connectionsNode) {
                JsonNode startNode = connNode.get("start");
                JsonNode endNode = connNode.get("end");

                String sourceNodeIdStr = startNode.get("id").asText();
                String targetNodeIdStr = endNode.get("id").asText();

                Long sourceNodeId;
                Long targetNodeId;

               

                if (sourceNodeIdStr.startsWith("TEMP")) {
                    if (!tempNodeIdToRealId.containsKey(sourceNodeIdStr)) {
                        logger.warn("Temporary source node ID not found in mapping: {}", sourceNodeIdStr);
                        continue;
                    }
                    sourceNodeId = tempNodeIdToRealId.get(sourceNodeIdStr);
                } else {
                    sourceNodeId = Long.parseLong(sourceNodeIdStr);
                }

               

                if (targetNodeIdStr.startsWith("TEMP")) {
                    if (!tempNodeIdToRealId.containsKey(targetNodeIdStr)) {
                        logger.warn("Temporary target node ID not found in mapping: {}", targetNodeIdStr);
                        continue;
                    }
                    targetNodeId = tempNodeIdToRealId.get(targetNodeIdStr);
                } else {
                    targetNodeId = Long.parseLong(targetNodeIdStr);
                }

               

                JsonNode metadataNode = connNode.get("metadata");
                float distance = metadataNode != null && metadataNode.has("distance") ?
                        (float) metadataNode.get("distance").asDouble() :
                        calculateDistance(startNode, endNode);

                boolean bidirectional = metadataNode == null || !metadataNode.has("bidirectional") ||
                        metadataNode.get("bidirectional").asBoolean();

               

                try {
                    nodeConnectionRepository.findBySourceNodeIdAndTargetNodeId(sourceNodeId, targetNodeId)
                            .ifPresent(conn -> {
                                logger.info("Connection already exists between nodes: {} -> {}", sourceNodeId, targetNodeId);
                                connectionDTOs.add(new NodeConnectionDTO(
                                        sourceNodeId,
                                        targetNodeId,
                                        conn.getDistance(),
                                        conn.isBidirectional()
                                ));
                            });
                } catch (Exception e) {
                    logger.warn("Error checking for existing connection: {} -> {}",
                            sourceNodeId, targetNodeId, e);
                }

               

                NodeConnectionDTO connectionDTO = new NodeConnectionDTO(
                        sourceNodeId,
                        targetNodeId,
                        distance,
                        bidirectional
                );

                try {
                    nodeService.createNodeConnection(connectionDTO);
                    logger.info("Created connection between nodes: {} -> {}", sourceNodeId, targetNodeId);
                    connectionDTOs.add(connectionDTO);
                } catch (Exception e) {
                    logger.warn("Error creating connection between nodes: {} -> {}", sourceNodeId, targetNodeId, e);
                }
            }

            return connectionDTOs;
        } catch (Exception e) {
            logger.error("Error processing connections", e);
            throw new RuntimeException("Failed to process connections", e);
        }
    }

    

    private float calculateDistance(JsonNode node1, JsonNode node2) {
        double x1 = node1.get("x").asDouble();
        double y1 = node1.get("y").asDouble();
        double x2 = node2.get("x").asDouble();
        double y2 = node2.get("y").asDouble();

        return (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
}