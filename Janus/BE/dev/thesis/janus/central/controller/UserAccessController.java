package dev.thesis.janus.central.controller;

import dev.thesis.janus.central.dto.*;
import dev.thesis.janus.central.dto.ProcessRequestDTO;
import dev.thesis.janus.central.exception.ResourceNotFoundException;
import dev.thesis.janus.central.model.UserRequestList;
import dev.thesis.janus.central.service.BuildingService;
import dev.thesis.janus.central.service.FloorService;
import dev.thesis.janus.central.service.UserPermissionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/protected")
@RequiredArgsConstructor
@Api(tags = "User Access Management")
public class UserAccessController {

    private final UserPermissionService userPermissionService;
    private final BuildingService buildingService;
    private final FloorService floorService;

    private static final Logger logger = LogManager.getLogger(UserAccessController.class);


    @GetMapping("/buildings/inaccessible")
    @ApiOperation("Get all buildings and rooms that a user does not have access to")
    public ResponseEntity<List<BuildingWithFloorsAndRoomsDTO>> getUserInaccessibleBuildings(
            @RequestAttribute(name = "userId", required = true) Long userId) {
        logger.info("User {} requested inaccessible buildings and rooms", userId);
        List<BuildingWithFloorsAndRoomsDTO> buildings = userPermissionService.getUserInaccessibleBuildingsWithRooms(userId);
        return ResponseEntity.ok(buildings);
    }
    @GetMapping("/permissions/check")
    @ApiOperation("Check if a user has permission to access a building or room")
    public ResponseEntity<Map<String, Boolean>> checkUserPermission(
            @RequestParam Long userId,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long mapObjectId) {

        boolean hasPermission = false;

        if (buildingId != null) {
            hasPermission = userPermissionService.canUserAccessBuilding(userId, buildingId);
            logger.info("Permission check for user {} on building {}: {}", userId, buildingId, hasPermission);
        } else if (mapObjectId != null) {
            hasPermission = userPermissionService.canUserAccessMapObject(userId, mapObjectId);
            logger.info("Permission check for user {} on map object {}: {}", userId, mapObjectId, hasPermission);
        }

        return ResponseEntity.ok(Map.of("hasPermission", hasPermission));
    }
    @GetMapping("/rooms/accessible")
    @ApiOperation("Get all rooms that a user has access to, organized by building")
    public ResponseEntity<BuildingsWithRoomsResponse> getUserAccessibleRooms(@RequestParam Long userId) {
        logger.info("User {} requested accessible rooms organized by building", userId);
        try {
            List<BuildingWithRoomsDTO> buildingsWithRooms = userPermissionService.getUserAccessibleBuildingsWithRooms(userId);
            BuildingsWithRoomsResponse response = new BuildingsWithRoomsResponse(buildingsWithRooms);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving accessible rooms for user {}", userId, e);
            throw new RuntimeException("Error retrieving accessible rooms: " + e.getMessage(), e);
        }
    }
    @GetMapping("/buildings/accessible")
    @ApiOperation("Get all buildings that a user has access to")
    public ResponseEntity<List<BuildingExtendedDTO>> getUserAccessibleBuildings(@RequestParam Long userId) {
        logger.info("User {} requested accessible buildings", userId);
        List<BuildingExtendedDTO> buildings = userPermissionService.getUserAccessibleBuildings(userId);
        return ResponseEntity.ok(buildings);
    }

    @GetMapping("/floors/accessible")
    @ApiOperation("Get all floors that a user has access to")
        public ResponseEntity<List<FloorDTO>> getUserAccessibleFloors(@RequestParam Long userId) {
            logger.info("User {} requested accessible floors", userId);
            List<FloorDTO> floors = userPermissionService.getUserAccessibleFloors(userId);
        return ResponseEntity.ok(floors);
    }
    @PostMapping("/grant-access-through-code")
    @ApiOperation("Grant a user access to a building or room using an access code")
    public ResponseEntity<Map<String, String>> grantAccessThroughCode(
            @RequestParam Long userId,
            @RequestParam String code) {

        logger.info("User {} redeeming access code", userId);

        try {
            userPermissionService.grantAccessThroughCode(userId, code);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Access granted successfully");

            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            logger.error("Error granting access: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (IllegalArgumentException e) {
            logger.error("Error granting access: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("Error granting access", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "An error occurred while granting access");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/request-access")
    public ResponseEntity<Map<String, Object>> createAccessRequest(
            @RequestBody AccessRequestDTO requestDTO) {

        Long userId = requestDTO.getUserId();
        Long buildingId = requestDTO.getBuildingId();
        String roomId = requestDTO.getRoomId();
        String accessType = requestDTO.getAccessType();

        Long requestId = userPermissionService.createAccessRequest(
                userId, buildingId, roomId, accessType
        );

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Access request created successfully");
        response.put("requestId", requestId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/process-request")
    @ApiOperation("Process (approve or deny) an access request")
    public ResponseEntity<Map<String, String>> processAccessRequest(
            @Valid @RequestBody ProcessRequestDTO requestDTO,
            @RequestAttribute(name = "userId", required = true) Long userId) {

        logger.info("Processing access request {}: {} by user {}",
                requestDTO.getRequestId(),
                requestDTO.getApproved() ? "approved" : "denied",
                userId);

        try {
           

            userPermissionService.processAccessRequest(
                    requestDTO.getRequestId(),
                    requestDTO.getApproved(),
                    userId);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", requestDTO.getApproved() ? "Request approved" : "Request denied");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing access request", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    @GetMapping("/admin/building-requests")
    @ApiOperation("Get all building access requests for buildings where the user is an admin")
    public ResponseEntity<BuildingRequestListDTO> getAdminBuildingRequests(
            @RequestAttribute(name = "userId", required = true) Long adminUserId) {

        logger.info("Admin user {} retrieving building requests", adminUserId);

        try {
            BuildingRequestListDTO requests = userPermissionService.getAdminBuildingRequestList(adminUserId);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            logger.error("Error retrieving admin building requests for user {}", adminUserId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    

    @GetMapping("/user/my-requests")
    @ApiOperation("Get all access requests made by the current user")
    public ResponseEntity<UserRequestListDTO> getUserRequests(
            @RequestAttribute(name = "userId", required = true) Long userId) {

        logger.info("User {} retrieving their requests", userId);

        try {
            UserRequestListDTO requests = userPermissionService.getUserRequestList(userId);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            logger.error("Error retrieving requests for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}