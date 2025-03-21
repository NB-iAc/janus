package dev.thesis.janus.central.service;

import dev.thesis.janus.central.exception.ResourceNotFoundException;
import dev.thesis.janus.central.repository.BuildingRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BuildingDeletionService {
    private static final Logger logger = LoggerFactory.getLogger(BuildingDeletionService.class);

    private final JdbcTemplate jdbcTemplate;
    private final BuildingRepository buildingRepository;

    

    @Transactional
    public void deleteBuildingEfficiently(Long buildingId) {
        logger.info("Starting efficient deletion of building with ID: {}", buildingId);

       

        if (!buildingRepository.existsById(buildingId)) {
            throw new ResourceNotFoundException("Building not found with id: " + buildingId);
        }

        try {
            long startTime = System.currentTimeMillis();

           


           

            int accessTokensDeleted = jdbcTemplate.update("DELETE FROM access_tokens WHERE building_id = ?", buildingId);

           

            int requestsDeleted = jdbcTemplate.update("DELETE FROM user_request_list WHERE building_id = ?", buildingId);

           

            int permissionsDeleted = jdbcTemplate.update("DELETE FROM user_building_permissions WHERE buildingid = ?", buildingId);

           

            int nodeConnectionsDeleted = jdbcTemplate.update(
                    "DELETE FROM node_connections WHERE source_node_id IN " +
                            "(SELECT id FROM nodes WHERE floor_id IN " +
                            "(SELECT id FROM floors WHERE building_id = ?))",
                    buildingId
            );

            nodeConnectionsDeleted += jdbcTemplate.update(
                    "DELETE FROM node_connections WHERE target_node_id IN " +
                            "(SELECT id FROM nodes WHERE floor_id IN " +
                            "(SELECT id FROM floors WHERE building_id = ?))",
                    buildingId
            );

           

            int nodeNeighborsDeleted = jdbcTemplate.update(
                    "DELETE FROM node_neighbors WHERE node_id IN " +
                            "(SELECT id FROM nodes WHERE floor_id IN " +
                            "(SELECT id FROM floors WHERE building_id = ?))",
                    buildingId
            );

            nodeNeighborsDeleted += jdbcTemplate.update(
                    "DELETE FROM node_neighbors WHERE neighbor_id IN " +
                            "(SELECT id FROM nodes WHERE floor_id IN " +
                            "(SELECT id FROM floors WHERE building_id = ?))",
                    buildingId
            );

           

            int mapObjectPointsDeleted = jdbcTemplate.update(
                    "DELETE FROM map_object_points WHERE map_object_id IN " +
                            "(SELECT id FROM map_objects WHERE floor_id IN " +
                            "(SELECT id FROM floors WHERE building_id = ?))",
                    buildingId
            );

           

            int roomPermissionsDeleted = jdbcTemplate.update(
                    "DELETE FROM user_room_permissions WHERE map_object_id IN " +
                            "(SELECT id FROM map_objects WHERE floor_id IN " +
                            "(SELECT id FROM floors WHERE building_id = ?))",
                    buildingId
            );

           

            int mapObjectsDeleted = jdbcTemplate.update(
                    "DELETE FROM map_objects WHERE floor_id IN " +
                            "(SELECT id FROM floors WHERE building_id = ?)",
                    buildingId
            );

           

            int nodesDeleted = jdbcTemplate.update(
                    "DELETE FROM nodes WHERE floor_id IN " +
                            "(SELECT id FROM floors WHERE building_id = ?)",
                    buildingId
            );

           

            int floorsDeleted = jdbcTemplate.update("DELETE FROM floors WHERE building_id = ?", buildingId);

           

            int logsDeleted = jdbcTemplate.update("DELETE FROM building_logs WHERE building_id = ?", buildingId);

           

            int buildingsDeleted = jdbcTemplate.update("DELETE FROM buildings WHERE id = ?", buildingId);

            long endTime = System.currentTimeMillis();
            logger.info("Successfully deleted building with ID: {} and all related data in {} ms",
                    buildingId, (endTime - startTime));
            logger.debug("Deletion summary - Building: {}, Floors: {}, Nodes: {}, MapObjects: {}, MapObjectPoints: {}, " +
                            "NodeConnections: {}, NodeNeighbors: {}, RoomPermissions: {}, BuildingPermissions: {}, " +
                            "AccessTokens: {}, Requests: {}, Logs: {}",
                    buildingsDeleted, floorsDeleted, nodesDeleted, mapObjectsDeleted, mapObjectPointsDeleted,
                    nodeConnectionsDeleted, nodeNeighborsDeleted, roomPermissionsDeleted, permissionsDeleted,
                    accessTokensDeleted, requestsDeleted, logsDeleted);

        } catch (Exception e) {
            logger.error("Error during efficient deletion of building with ID: {}", buildingId, e);
            throw new RuntimeException("Failed to delete building: " + e.getMessage(), e);
        }
    }
}