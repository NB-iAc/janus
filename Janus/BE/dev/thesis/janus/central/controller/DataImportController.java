package dev.thesis.janus.central.controller;

import dev.thesis.janus.central.dto.*;
import dev.thesis.janus.central.exception.ResourceNotFoundException;
import dev.thesis.janus.central.model.*;
import dev.thesis.janus.central.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class DataImportController {

    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final NodeRepository nodeRepository;
    private final MapObjectRepository mapObjectRepository;
    private final MapObjectPointRepository mapObjectPointRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final UserRepository userRepository;
    private final UserBuildingPermissionsRepository userBuildingPermissionsRepository;

    @PostMapping("/building")
    @Transactional
    public ResponseEntity<Map<String, Object>> importBuilding(@RequestBody Map<String, Object> importData) {
        try {
           

            Building building = new Building();
            Map<String, Object> buildingData = (Map<String, Object>) importData.get("building");
            building.setName((String) buildingData.getOrDefault("name", "Demo Building"));
            building.setDescription((String) buildingData.getOrDefault("description", "Imported from Kotlin app"));
            Building savedBuilding = buildingRepository.save(building);

           

            User owner = userRepository.findById(1L)
                    .orElseThrow(() -> new ResourceNotFoundException("User with ID 1 not found"));

            UserBuildingPermissions permission = new UserBuildingPermissions();
            permission.setUser(owner);
            permission.setBuilding(savedBuilding);
            permission.setAllAccess(true);
            userBuildingPermissionsRepository.save(permission);

           

            Map<Integer, Floor> floorMap = new HashMap<>();
            List<Map<String, Object>> floorsData = (List<Map<String, Object>>) importData.get("floors");
            for (Map<String, Object> floorData : floorsData) {
                Floor floor = new Floor();
                floor.setBuilding(savedBuilding);
                floor.setFloorNumber((Integer) floorData.get("floorNumber"));
                floor.setDisplayName((String) floorData.get("displayName"));
                floor.setAccessible((Boolean) floorData.getOrDefault("accessible", true));
                Floor savedFloor = floorRepository.save(floor);
                floorMap.put(floor.getFloorNumber(), savedFloor);
            }

           

           

            Map<Integer, Map<String, Node>> floorNodeMap = new HashMap<>();
            for (Integer floorNumber : floorMap.keySet()) {
                floorNodeMap.put(floorNumber, new HashMap<>());
            }

           

            Map<String, Integer> nodeIdToFloorMap = new HashMap<>();

           

            Map<String, Node> globalNodeMap = new HashMap<>();

           

            List<Map<String, Object>> nodesData = (List<Map<String, Object>>) importData.get("nodes");
            System.out.println("Processing " + nodesData.size() + " nodes...");

            for (Map<String, Object> nodeData : nodesData) {
                String originalId = (String) nodeData.get("originalId");
                Integer floorNumber = (Integer) nodeData.get("floorNumber");

                if (!floorMap.containsKey(floorNumber)) {
                    System.out.println("Skipping node with invalid floor: " + originalId + ", floor: " + floorNumber);
                    continue;
                }

                Node node = new Node();
                node.setX(((Number) nodeData.get("x")).floatValue());
                node.setY(((Number) nodeData.get("y")).floatValue());
                node.setFloor(floorMap.get(floorNumber));
                node.setElevationNode((Boolean) nodeData.getOrDefault("isElevationNode", false));
                node.setNodeType((String) nodeData.getOrDefault("nodeType", ""));
                Node savedNode = nodeRepository.save(node);

               

                floorNodeMap.get(floorNumber).put(originalId, savedNode);
                nodeIdToFloorMap.put(originalId, floorNumber);
                globalNodeMap.put(originalId, savedNode);

                System.out.println("Created node: ID=" + originalId +
                        ", DB_ID=" + savedNode.getId() +
                        ", Floor=" + floorNumber);
            }

           

            int objectCounter = 0;
            List<Map<String, Object>> mapObjectsData = (List<Map<String, Object>>) importData.get("mapObjects");
            if (mapObjectsData != null) {
                System.out.println("Processing " + mapObjectsData.size() + " map objects...");

                for (Map<String, Object> mapObjectData : mapObjectsData) {
                    Integer floorNumber = (Integer) mapObjectData.get("floorNumber");

                    if (!floorMap.containsKey(floorNumber)) {
                        continue;
                    }

                    MapObject mapObject = new MapObject();
                    mapObject.setFloor(floorMap.get(floorNumber));
                    mapObject.setObjectType(MapObjectType.valueOf((String) mapObjectData.get("objectType")));
                    mapObject.setName((String) mapObjectData.getOrDefault("name", ""));

                   

                    String roomId = "IMPORT-" + savedBuilding.getId() + "-" + objectCounter++;
                    mapObject.setRoomId(roomId);

                    mapObject.setCategory((String) mapObjectData.getOrDefault("category", "DEFAULT"));
                    mapObject.setContactDetails((String) mapObjectData.getOrDefault("contactDetails", ""));
                    mapObject.setRoomType((String) mapObjectData.getOrDefault("roomType", ""));
                    mapObject.setDescription((String) mapObjectData.getOrDefault("description", ""));
                    mapObject.setAccessible((Boolean) mapObjectData.getOrDefault("accessible", true));

                   

                    String entranceNodeId = (String) mapObjectData.get("entranceNodeId");
                    if (entranceNodeId != null && !entranceNodeId.isEmpty() && globalNodeMap.containsKey(entranceNodeId)) {
                        mapObject.setEntranceNode(globalNodeMap.get(entranceNodeId));
                    }

                    MapObject savedMapObject = mapObjectRepository.save(mapObject);

                   

                    List<Map<String, Object>> pointsData = (List<Map<String, Object>>) mapObjectData.get("points");
                    if (pointsData != null) {
                        List<MapObjectPoint> points = new ArrayList<>();
                        for (int i = 0; i < pointsData.size(); i++) {
                            Map<String, Object> pointData = pointsData.get(i);

                            MapObjectPoint point = new MapObjectPoint();
                            point.setMapObject(savedMapObject);
                            point.setX(((Number) pointData.get("x")).floatValue());
                            point.setY(((Number) pointData.get("y")).floatValue());
                            point.setPointOrder(i);
                            points.add(point);
                        }
                        mapObjectPointRepository.saveAll(points);
                    }
                }
            }

           

            List<Map<String, Object>> connectionsData = (List<Map<String, Object>>) importData.get("connections");
            if (connectionsData != null && !connectionsData.isEmpty()) {
                System.out.println("Processing " + connectionsData.size() + " connections...");

               

                List<NodeConnection> connections = new ArrayList<>();

               

                Map<Long, Set<Node>> nodeNeighbors = new HashMap<>();

                int processedCount = 0;
                int skippedCount = 0;

                for (Map<String, Object> connectionData : connectionsData) {
                    try {
                        String sourceNodeId = (String) connectionData.get("sourceNodeId");
                        String targetNodeId = (String) connectionData.get("targetNodeId");

                        if (sourceNodeId == null || targetNodeId == null) {
                            System.out.println("Skipping connection: Missing node IDs");
                            skippedCount++;
                            continue;
                        }

                       

                        Integer sourceFloorNumber = nodeIdToFloorMap.get(sourceNodeId);
                        Integer targetFloorNumber = nodeIdToFloorMap.get(targetNodeId);

                       

                        if (sourceFloorNumber == null) {
                            sourceFloorNumber = (Integer) connectionData.get("sourceFloorNumber");
                        }
                        if (sourceFloorNumber == null) {
                            sourceFloorNumber = (Integer) connectionData.get("floorNumber");
                        }

                        if (targetFloorNumber == null) {
                            targetFloorNumber = (Integer) connectionData.get("targetFloorNumber");
                        }
                        if (targetFloorNumber == null && sourceFloorNumber != null) {
                            targetFloorNumber = sourceFloorNumber;

                        }

                       

                        if (sourceFloorNumber == null || targetFloorNumber == null) {
                            System.out.println("Skipping connection due to missing floor numbers for nodes: " +
                                    "source=" + sourceNodeId + ", target=" + targetNodeId);
                            skippedCount++;
                            continue;
                        }

                       

                        Node sourceNode = globalNodeMap.get(sourceNodeId);
                        Node targetNode = globalNodeMap.get(targetNodeId);

                       

                        if (sourceNode == null && floorNodeMap.containsKey(sourceFloorNumber)) {
                            sourceNode = floorNodeMap.get(sourceFloorNumber).get(sourceNodeId);
                        }

                        if (targetNode == null && floorNodeMap.containsKey(targetFloorNumber)) {
                            targetNode = floorNodeMap.get(targetFloorNumber).get(targetNodeId);
                        }

                       

                        if (sourceNode == null || targetNode == null) {
                            System.out.println("Skipping connection due to nodes not found: " +
                                    "sourceId=" + sourceNodeId + ", targetId=" + targetNodeId);
                            skippedCount++;
                            continue;
                        }

                       

                        if (!nodeNeighbors.containsKey(sourceNode.getId())) {
                            nodeNeighbors.put(sourceNode.getId(), new HashSet<>());
                        }
                        nodeNeighbors.get(sourceNode.getId()).add(targetNode);

                        boolean bidirectional = (Boolean) connectionData.getOrDefault("bidirectional", true);
                        if (bidirectional) {
                            if (!nodeNeighbors.containsKey(targetNode.getId())) {
                                nodeNeighbors.put(targetNode.getId(), new HashSet<>());
                            }
                            nodeNeighbors.get(targetNode.getId()).add(sourceNode);
                        }

                       

                        NodeConnection connection = new NodeConnection();
                        connection.setSourceNode(sourceNode);
                        connection.setTargetNode(targetNode);
                       

                        connection.setDistance(((Number) connectionData.getOrDefault("distance", 1.0)).floatValue());
                        connection.setBidirectional(bidirectional);
                        connections.add(connection);

                        processedCount++;
                    } catch (Exception e) {
                        System.out.println("Error processing connection: " + e.getMessage());
                        e.printStackTrace();
                        skippedCount++;
                    }
                }

               

                System.out.println("Updating node neighbors...");
                for (Map.Entry<Long, Set<Node>> entry : nodeNeighbors.entrySet()) {
                    Node node = nodeRepository.findById(entry.getKey())
                            .orElse(null);

                    if (node != null) {
                       

                        node.getNeighbors().addAll(entry.getValue());
                        nodeRepository.save(node);
                    }
                }

               

                if (!connections.isEmpty()) {
                    System.out.println("Saving " + connections.size() + " node connections...");
                    nodeConnectionRepository.saveAll(connections);
                    System.out.println("Successfully processed " + processedCount + " connections, skipped " + skippedCount);
                } else {
                    System.out.println("No valid connections to save. Skipped " + skippedCount + " connections.");
                }
            } else {
                System.out.println("No connection data provided in the import");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("buildingId", savedBuilding.getId());
            response.put("message", "Building imported successfully and assigned to user ID 1");
            response.put("importDetails", Map.of(
                    "floors", floorMap.size(),
                    "nodes", globalNodeMap.size(),
                    "objects", objectCounter
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("stackTrace", Arrays.toString(e.getStackTrace()));

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}