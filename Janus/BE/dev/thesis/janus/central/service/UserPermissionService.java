package dev.thesis.janus.central.service;

import dev.thesis.janus.central.dto.*;
import dev.thesis.janus.central.exception.ResourceNotFoundException;
import dev.thesis.janus.central.model.*;
import dev.thesis.janus.central.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPermissionService {

    private static final Logger logger = LoggerFactory.getLogger(UserPermissionService.class);

    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final MapObjectRepository mapObjectRepository;
    private final UserBuildingPermissionsRepository userBuildingPermissionsRepository;
    private final UserRoomPermissionsRepository userRoomPermissionsRepository;
    private final UserRequestListRepository userRequestListRepository;
    private final EntityManager entityManager;
    private final MapObjectService mapObjectService;
    private final BuildingLogService buildingLogService;
    

    @Transactional(readOnly = true)
    public boolean canUserAccessBuilding(Long userId, Long buildingId) {
        logger.debug("Checking if user {} can access building {}", userId, buildingId);

       

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

       

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + buildingId));

       

        Optional<UserBuildingPermissions> permission = userBuildingPermissionsRepository.findByUserIdAndBuildingId(userId, buildingId);

        boolean hasAccess = permission.isPresent();
        logger.debug("User {} {} access to building {}", userId, hasAccess ? "has" : "does not have", buildingId);

        return hasAccess;
    }

    

    @Transactional(readOnly = true)
    public boolean canUserAccessMapObject(Long userId, Long mapObjectId) {
        logger.debug("Checking if user {} can access map object {}", userId, mapObjectId);

       

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

       

        Optional<MapObject> mapObjectOptional = mapObjectRepository.findById(mapObjectId);

        if (mapObjectOptional.isEmpty()) {
            logger.warn("Map object {} not found", mapObjectId);
            return false;
        }

        MapObject mapObject = mapObjectOptional.get();
        Long buildingId = mapObject.getFloor().getBuilding().getId();

       

        if (canUserAccessBuilding(userId, buildingId)) {
            logger.debug("User {} has building-level access to map object {}", userId, mapObjectId);
            return true;
        }

       

        Optional<UserRoomPermissions> permission = userRoomPermissionsRepository.findByUserIdAndMapObjectId(userId, mapObjectId);

        boolean hasAccess = permission.isPresent();
        logger.debug("User {} {} specific access to map object {}", userId, hasAccess ? "has" : "does not have", mapObjectId);

        return hasAccess;
    }

    

    @Transactional(readOnly = true)
    public List<MapObjectDTO> getUserAccessibleRooms(Long userId) {
        logger.debug("Retrieving rooms accessible by user {}", userId);

       

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

       

        List<MapObject> accessibleRooms = new ArrayList<>();

       

        List<MapObject> directRoomPermissions = userRoomPermissionsRepository.findByUserId(userId)
                .stream()
                .map(UserRoomPermissions::getMapObject)
                .filter(mapObject -> mapObject.getObjectType() == MapObjectType.ROOM)
                .collect(Collectors.toList());

        accessibleRooms.addAll(directRoomPermissions);

       

        List<Long> accessibleBuildingIds = userBuildingPermissionsRepository.findByUserId(userId)
                .stream()
                .map(permission -> permission.getBuilding().getId())
                .collect(Collectors.toList());

       

        for (Long buildingId : accessibleBuildingIds) {
            List<MapObject> buildingRooms = mapObjectRepository.findAllByBuildingId(buildingId)
                    .stream()
                    .filter(mapObject -> mapObject.getObjectType() == MapObjectType.ROOM)
                    .collect(Collectors.toList());

            accessibleRooms.addAll(buildingRooms);
        }

       

        accessibleRooms = accessibleRooms.stream()
                .distinct()
                .collect(Collectors.toList());

       

        List<MapObjectDTO> roomDTOs = accessibleRooms.stream()
                .map(mapObjectService::convertToDTO)
                .collect(Collectors.toList());

        logger.debug("Found {} rooms accessible by user {}", roomDTOs.size(), userId);

        return roomDTOs;
    }
    @Transactional(readOnly = true)
    public List<BuildingExtendedDTO> getUserAccessibleBuildings(Long userId) {
        logger.debug("Retrieving extended building information for user {}", userId);

       

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

       

        List<Object[]> results = entityManager.createNativeQuery(
                        "WITH admin_users AS (" +
                                "   SELECT ubp.buildingid, u.username, u.email " +
                                "   FROM user_building_permissions ubp " +
                                "   JOIN users u ON ubp.userid = u.userid " +
                                "   WHERE ubp.all_access = true " +
                                ") " +
                                "SELECT b.id, b.name, b.description, b.created_at, b.updated_at, " +
                                "       (SELECT COUNT(*) FROM floors f WHERE f.building_id = b.id) AS floor_count, " +
                                "       string_agg(DISTINCT au.username, ',') AS admin_names, " +
                                "       string_agg(DISTINCT au.email, ',') AS admin_emails " +
                                "FROM user_building_permissions ubp " +
                                "JOIN buildings b ON ubp.buildingid = b.id " +
                                "LEFT JOIN admin_users au ON b.id = au.buildingid " +
                                "WHERE ubp.userid = :userId " +
                                "GROUP BY b.id, b.name, b.description, b.created_at, b.updated_at " +
                                "ORDER BY b.name"
                ).setParameter("userId", userId)
                .getResultList();

       

        List<BuildingExtendedDTO> buildingDTOs = new ArrayList<>();

        for (Object[] row : results) {
            Long id = ((Number) row[0]).longValue();
            String name = (String) row[1];
            String description = (String) row[2];
            LocalDateTime createdAt = row[3] != null ? ((Timestamp) row[3]).toLocalDateTime() : null;
            LocalDateTime updatedAt = row[4] != null ? ((Timestamp) row[4]).toLocalDateTime() : null;
            int floorCount = ((Number) row[5]).intValue();

           

            List<AdminContactDTO> adminContacts = new ArrayList<>();
            String adminNames = (String) row[6];
            String adminEmails = (String) row[7];

            if (adminNames != null && adminEmails != null) {
                String[] names = adminNames.split(",");
                String[] emails = adminEmails.split(",");

                for (int i = 0; i < Math.min(names.length, emails.length); i++) {
                    adminContacts.add(new AdminContactDTO(names[i], emails[i]));
                }
            }

           

            List<BuildingLogDTO> buildingLogs = buildingLogService.getBuildingLogs(id);

            BuildingExtendedDTO dto = new BuildingExtendedDTO(
                    id, name, description, createdAt, updatedAt, floorCount, adminContacts, buildingLogs
            );
            buildingDTOs.add(dto);
        }

        logger.debug("Found {} buildings accessible by user {}", buildingDTOs.size(), userId);
        return buildingDTOs;
    }
    @Transactional(readOnly = true)
    public List<FloorDTO> getUserAccessibleFloors(Long userId) {
        logger.debug("Retrieving floors accessible by user {}", userId);

       

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

       

        List<Long> accessibleBuildingIds = userBuildingPermissionsRepository.findByUserId(userId)
                .stream()
                .map(permission -> permission.getBuilding().getId())
                .collect(Collectors.toList());

       

        List<Floor> accessibleFloors = new ArrayList<>();
        for (Long buildingId : accessibleBuildingIds) {
            accessibleFloors.addAll(floorRepository.findByBuildingId(buildingId));
        }

       

        List<FloorDTO> floorDTOs = accessibleFloors.stream()
                .map(this::convertFloorToDTO)
                .collect(Collectors.toList());

        logger.debug("Found {} floors accessible by user {}", floorDTOs.size(), userId);

        return floorDTOs;
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
    @Autowired
    private AccessTokenRepository accessTokenRepository;

    @Transactional
    public void grantAccessThroughCode(Long userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        AccessToken accessToken = accessTokenRepository.findByToken(code)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid access code"));

        if (accessToken.getExpirationDate() != null &&
                accessToken.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Access code has expired");
        }

        if (accessToken.getBuilding() != null && accessToken.getMapObject() == null) {
            grantBuildingAccess(user, accessToken.getBuilding(), true);
        } else if (accessToken.getBuilding() == null && accessToken.getMapObject() != null) {
            grantRoomAccess(user, accessToken.getMapObject());
        } else {
            throw new IllegalArgumentException("Invalid access code configuration");
        }
    }

    private void grantBuildingAccess(User user, Building building, boolean allAccess) {
        Optional<UserBuildingPermissions> existingPermission =
                userBuildingPermissionsRepository.findByUserIdAndBuildingId(user.getUserid(), building.getId());

        if (existingPermission.isEmpty()) {
            UserBuildingPermissions newPermission = new UserBuildingPermissions();
            newPermission.setUser(user);
            newPermission.setBuilding(building);
            newPermission.setAllAccess(allAccess);
            userBuildingPermissionsRepository.save(newPermission);
            logger.info("Granted building access (allAccess={}) to user {} for building {}",
                    allAccess, user.getUserid(), building.getId());
        } else {
            UserBuildingPermissions permission = existingPermission.get();
            if (allAccess && !permission.isAllAccess()) {
                permission.setAllAccess(true);
                userBuildingPermissionsRepository.save(permission);
                logger.info("Upgraded user {} to all access for building {}",
                        user.getUserid(), building.getId());
            } else {
                logger.info("User {} already has appropriate access to building {}",
                        user.getUserid(), building.getId());
            }
        }
    }

    private void grantRoomAccess(User user, MapObject room) {
        if (room.getObjectType() != MapObjectType.ROOM) {
            throw new IllegalArgumentException("Map object is not a room");
        }

        Optional<UserRoomPermissions> existingPermission =
                userRoomPermissionsRepository.findByUserIdAndMapObjectId(user.getUserid(), room.getId());

        if (existingPermission.isEmpty()) {
            UserRoomPermissions newPermission = new UserRoomPermissions();
            newPermission.setUser(user);
            newPermission.setMapObject(room);
            userRoomPermissionsRepository.save(newPermission);
            logger.info("Granted room access to user {} for room {}", user.getUserid(), room.getId());
        } else {
            logger.info("User {} already has access to room {}", user.getUserid(), room.getId());
        }
    }

    @Transactional
    public Long createAccessRequest(Long userId, Long buildingId, String roomId, String requestType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + buildingId));

        List<UserRequestList> existingRequests = userRequestListRepository
                .findByUserIdAndBuildingIdAndRoomId(userId, buildingId, roomId);

        if (!existingRequests.isEmpty()) {
            for (UserRequestList request : existingRequests) {
                if ("pending".equalsIgnoreCase(request.getStatus())) {
                    return request.getRequestid();
                }
            }
        }

        UserRequestList request = new UserRequestList();
        request.setRequestingUser(user);
        request.setBuilding(building);
        request.setRoomId(roomId);
        request.setAccessType(requestType);
        request.setStatus("pending");

        UserRequestList savedRequest = userRequestListRepository.save(request);
        logger.info("Access request created: User {} requesting {} access to building {} room {}",
                userId, requestType, buildingId, roomId);

        return savedRequest.getRequestid();
    }

    @Transactional
    public void processAccessRequest(Long requestId, boolean approved, Long processorId) {
        UserRequestList request = userRequestListRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + requestId));

       

        Building building = request.getBuilding();
        boolean isAdmin = userBuildingPermissionsRepository.findByUserIdAndBuildingId(processorId, building.getId())
                .map(UserBuildingPermissions::isAllAccess)
                .orElse(false);

        if (!isAdmin) {
            throw new IllegalArgumentException("You don't have permission to process requests for this building");
        }

        request.setStatus(approved ? "approved" : "denied");
        userRequestListRepository.save(request);
        if (approved) {
            User requestingUser = request.getRequestingUser();
            String roomId = request.getRoomId();
            boolean allAccess = "full".equalsIgnoreCase(request.getAccessType());
            if (roomId != null && !roomId.isEmpty()) {
                try {
                   

                    Long mapObjectId = Long.parseLong(roomId);
                    MapObject mapObject = mapObjectRepository.findById(mapObjectId)
                            .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + mapObjectId));

                    grantRoomAccess(requestingUser, mapObject);
                    logger.info("Room access granted to user {} for room {} by user {}",
                            requestingUser.getUserid(), mapObjectId, processorId);
                } catch (NumberFormatException e) {
                   

                    MapObject mapObject = mapObjectRepository.findByRoomId(roomId)
                            .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

                    grantRoomAccess(requestingUser, mapObject);
                    logger.info("Room access granted to user {} for room {} by user {}",
                            requestingUser.getUserid(), roomId, processorId);
                }
            } else {
                grantBuildingAccess(requestingUser, building, allAccess);
                logger.info("Building access (allAccess={}) granted to user {} for building {} by user {}",
                        allAccess, requestingUser.getUserid(), building.getId(), processorId);
            }
        } else {
            logger.info("Access request {} denied by user {}", requestId, processorId);
        }
    }
    @Transactional
    public boolean revokeTenantPermission(Long adminUserId, Long userRoomPermissionsId) {
        logger.debug("Admin user {} attempting to revoke tenant permission {}", adminUserId, userRoomPermissionsId);

       

        UserRoomPermissions permission = userRoomPermissionsRepository.findById(userRoomPermissionsId)
                .orElseThrow(() -> new ResourceNotFoundException("User room permission not found with id: " + userRoomPermissionsId));

       

        MapObject mapObject = permission.getMapObject();
        Building building = mapObject.getFloor().getBuilding();
        Long buildingId = building.getId();

       

        boolean isAdmin = userBuildingPermissionsRepository.findByUserIdAndBuildingId(adminUserId, buildingId)
                .map(UserBuildingPermissions::isAllAccess)
                .orElse(false);

        if (!isAdmin) {
            logger.warn("User {} attempted to revoke permission {} without admin rights", adminUserId, userRoomPermissionsId);
            throw new IllegalArgumentException("You don't have admin rights for this building");
        }

       

        Long tenantUserId = permission.getUser().getUserid();
        String tenantUsername = permission.getUser().getUsername();
        String roomName = mapObject.getName();

       

        userRoomPermissionsRepository.delete(permission);

        logger.info("Admin user {} revoked access for user {} (ID: {}) to room {} in building {}",
                adminUserId, tenantUsername, tenantUserId, roomName, buildingId);

        return true;
    }

    @Transactional(readOnly = true)
    public List<TenantDTO> getTenantsByAdminId(Long adminUserId) {
        logger.debug("Retrieving tenants for buildings managed by admin user {}", adminUserId);

       

        List<UserBuildingPermissions> adminPermissions = userBuildingPermissionsRepository.findByUserId(adminUserId)
                .stream()
                .filter(UserBuildingPermissions::isAllAccess)
                .collect(Collectors.toList());

        if (adminPermissions.isEmpty()) {
            logger.info("User {} is not an admin for any buildings", adminUserId);
            return Collections.emptyList();
        }

       

        List<Long> adminBuildingIds = adminPermissions.stream()
                .map(permission -> permission.getBuilding().getId())
                .collect(Collectors.toList());

        logger.debug("User {} is admin for buildings: {}", adminUserId, adminBuildingIds);

       

        String sql = "SELECT " +
                "  urp.userroompermissionsid, " +
                "  u.userid, u.username, u.email, " +
                "  b.id AS building_id, b.name AS building_name, " +
                "  f.floor_number, f.display_name AS floor_name, " +
                "  mo.id AS room_id, mo.name AS room_name " +
                "FROM " +
                "  user_room_permissions urp " +
                "  JOIN users u ON urp.userid = u.userid " +
                "  JOIN map_objects mo ON urp.map_object_id = mo.id " +
                "  JOIN floors f ON mo.floor_id = f.id " +
                "  JOIN buildings b ON f.building_id = b.id " +
                "WHERE " +
                "  b.id IN (:buildingIds) " +
                "  AND NOT EXISTS ( " +
                "    SELECT 1 FROM user_building_permissions ubp " +
                "    WHERE ubp.userid = u.userid AND ubp.buildingid = b.id " +
                "  ) " + 

                "ORDER BY " +
                "  b.name, f.floor_number, mo.name, u.username";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("buildingIds", adminBuildingIds);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<TenantDTO> tenants = new ArrayList<>();

        for (Object[] row : results) {
            int idx = 0;
            Long userRoomPermissionsId = ((Number) row[idx++]).longValue();
            Long userId = ((Number) row[idx++]).longValue();
            String userName = (String) row[idx++];
            String userEmail = (String) row[idx++];
            Long buildingId = ((Number) row[idx++]).longValue();
            String buildingName = (String) row[idx++];
            Integer floorNumber = ((Number) row[idx++]).intValue();
            String floorName = (String) row[idx++];
            Long roomId = ((Number) row[idx++]).longValue();
            String roomName = (String) row[idx++];

            TenantDTO tenant = new TenantDTO(
                    userId,
                    userName,
                    userEmail,
                    buildingId,
                    buildingName,
                    floorNumber,
                    floorName,
                    roomId,
                    roomName,
                    userRoomPermissionsId
            );

            tenants.add(tenant);
        }

        logger.info("Found {} tenants for buildings managed by admin user {}", tenants.size(), adminUserId);
        return tenants;
    }
    @Transactional(readOnly = true)
    public BuildingRequestListDTO getAdminBuildingRequestList(Long adminUserId) {
        logger.debug("Retrieving building requests for admin user {}", adminUserId);

       

        List<UserBuildingPermissions> adminPermissions = userBuildingPermissionsRepository.findByUserId(adminUserId)
                .stream()
                .filter(UserBuildingPermissions::isAllAccess)
                .collect(Collectors.toList());

        if (adminPermissions.isEmpty()) {
            logger.info("User {} is not an admin for any buildings", adminUserId);
            return new BuildingRequestListDTO(Collections.emptyList());
        }

       

        List<Long> adminBuildingIds = adminPermissions.stream()
                .map(permission -> permission.getBuilding().getId())
                .collect(Collectors.toList());

        logger.debug("User {} is admin for buildings: {}", adminUserId, adminBuildingIds);

       

        String sql = "SELECT " +
                "  req.requestid, " +
                "  u.userid, u.username, u.email, " +
                "  b.id, b.name, " +
                "  req.accesstype, req.roomid, req.status " +
                "FROM " +
                "  user_request_list req " +
                "  JOIN users u ON req.requesting_user_id = u.userid " +
                "  JOIN buildings b ON req.building_id = b.id " +
                "WHERE " +
                "  req.building_id IN (:buildingIds) " +
                "  AND req.status = 'pending' " + 

                "ORDER BY " +
                "  req.requestid DESC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("buildingIds", adminBuildingIds);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<BuildingRequestItemDTO> requestItems = new ArrayList<>();

        for (Object[] row : results) {
            int idx = 0;
            Long requestId = ((Number) row[idx++]).longValue();
            Long userId = ((Number) row[idx++]).longValue();
            String userName = (String) row[idx++];
            String userEmail = (String) row[idx++];
            Long buildingId = ((Number) row[idx++]).longValue();
            String buildingName = (String) row[idx++];
            String accessType = (String) row[idx++];
            String roomId = (String) row[idx++];
            String status = (String) row[idx++];

            RequestUserDTO userDTO = new RequestUserDTO(userId, userName, userEmail);

            BuildingRequestItemDTO item = new BuildingRequestItemDTO(
                    requestId, userDTO, buildingId, buildingName, accessType, roomId, status
            );

            requestItems.add(item);
        }

        logger.info("Found {} building requests for admin user {}", requestItems.size(), adminUserId);
        return new BuildingRequestListDTO(requestItems);
    }

    

    @Transactional(readOnly = true)
    public UserRequestListDTO getUserRequestList(Long userId) {
        logger.debug("Retrieving request list for user {}", userId);

       

        String sql = "SELECT req.requestid, b.id, b.name, req.accesstype, req.roomid, mo.name AS room_name, req.status FROM user_request_list req JOIN buildings b ON req.building_id = b.id LEFT JOIN map_objects mo ON CAST(mo.id AS VARCHAR) = req.roomid WHERE req.requesting_user_id = ?1 ORDER BY req.status, req.requestid DESC;";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, userId);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<UserRequestItemDTO> requestItems = new ArrayList<>();

        for (Object[] row : results) {
            int idx = 0;
            Long requestId = ((Number) row[idx++]).longValue();
            Long buildingId = ((Number) row[idx++]).longValue();
            String buildingName = (String) row[idx++];
            String accessType = (String) row[idx++];
            String roomId = (String) row[idx++];
            String roomName = (String) row[idx++];
            String status = (String) row[idx++];

            UserRequestItemDTO item = new UserRequestItemDTO(
                    requestId, buildingId, buildingName, accessType, roomId, roomName, status
            );

            requestItems.add(item);
        }

        logger.info("Found {} requests for user {}", requestItems.size(), userId);
        return new UserRequestListDTO(requestItems);
    }
    @Transactional(readOnly = true)
    public List<BuildingWithRoomsDTO> getUserAccessibleBuildingsWithRooms(Long userId) {
        logger.debug("Retrieving buildings and rooms accessible by user {}", userId);

       

       

        String sql = "WITH user_buildings AS ( SELECT DISTINCT b.id, b.name, b.description FROM user_building_permissions ubp JOIN buildings b ON ubp.buildingid = b.id WHERE ubp.userid = ?1 ), user_building_rooms AS ( SELECT DISTINCT CAST(mo.id AS VARCHAR) AS room_id, mo.room_id AS room_code, mo.name, mo.description, mo.category, mo.room_type, mo.contact_details, mo.accessible, mo.entrance_node_id, f.id AS floor_id, f.floor_number, f.display_name AS floor_name, b.id AS building_id, b.name AS building_name, b.description AS building_description, 'building' AS access_source FROM user_buildings ub JOIN buildings b ON ub.id = b.id JOIN floors f ON f.building_id = b.id JOIN map_objects mo ON mo.floor_id = f.id WHERE mo.object_type = 'ROOM' ), direct_room_access AS ( SELECT DISTINCT CAST(mo.id AS VARCHAR) AS room_id, mo.room_id AS room_code, mo.name, mo.description, mo.category, mo.room_type, mo.contact_details, mo.accessible, mo.entrance_node_id, f.id AS floor_id, f.floor_number, f.display_name AS floor_name, b.id AS building_id, b.name AS building_name, b.description AS building_description, 'direct' AS access_source FROM user_room_permissions urp JOIN map_objects mo ON urp.map_object_id = mo.id JOIN floors f ON mo.floor_id = f.id JOIN buildings b ON f.building_id = b.id WHERE urp.userid = ?1 AND mo.object_type = 'ROOM' ), combined_results AS ( SELECT *, ROW_NUMBER() OVER (PARTITION BY room_id ORDER BY access_source) AS rn FROM ( SELECT * FROM user_building_rooms UNION ALL SELECT * FROM direct_room_access ) all_rooms ) SELECT building_id, building_name, building_description, room_id, room_code, name, description, floor_id, floor_number, floor_name, category, room_type, contact_details, accessible, entrance_node_id FROM combined_results WHERE rn = 1 ORDER BY building_name, name";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, userId);

        Map<Long, BuildingWithRoomsDTO> buildingsMap = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        for (Object[] row : results) {
            int col = 0;
            Long buildingId = ((Number) row[col++]).longValue();
            String buildingName = (String) row[col++];
            String buildingDesc = (String) row[col++];

           

            BuildingWithRoomsDTO building = buildingsMap.computeIfAbsent(
                    buildingId,
                    id -> new BuildingWithRoomsDTO(
                            id,
                            buildingName != null ? buildingName : "",
                            buildingDesc != null ? buildingDesc : "",
                            new ArrayList<>()
                    )
            );

           

            if (row[col] == null) {
                continue;
            }

            String stringRoomId = (String) row[col++];
            Long roomId ;
            try {
                roomId = (Long.parseLong(stringRoomId));

            }catch (NumberFormatException e){
                continue;
            }
            String roomCode = (String) row[col++];
            String roomName = (String) row[col++];
            String roomDesc = (String) row[col++];
            Long floorId = ((Number) row[col++]).longValue();
            Integer floorNumber = ((Number) row[col++]).intValue();
            String floorName = (String) row[col++];
            String category = (String) row[col++];
            String roomType = (String) row[col++];
            String contactDetails = (String) row[col++];
            Boolean accessible = (Boolean) row[col++];
            Object entranceNodeObj = row[col++];
            Long entranceNodeId = entranceNodeObj != null ? ((Number) entranceNodeObj).longValue() : null;

           

            boolean roomExists = building.getRooms().stream()
                    .anyMatch(r -> roomId.equals(r.getId()));

            if (!roomExists) {
                RoomDTO room = new RoomDTO(
                        roomId,
                        roomCode != null ? roomCode : "",
                        roomName != null ? roomName : "",
                        roomDesc != null ? roomDesc : "",
                        floorId,
                        floorNumber,
                        floorName != null ? floorName : "",
                        category != null ? category : "",
                        roomType != null ? roomType : "",
                        contactDetails != null ? contactDetails : "",
                        accessible != null ? accessible : false,
                        entranceNodeId
                );

                building.getRooms().add(room);
            }
        }

       

        if (buildingsMap.isEmpty()) {
            addDirectRoomAccessBuildings(userId, buildingsMap);
        }

        logger.debug("Found {} buildings with rooms accessible by user {}", buildingsMap.size(), userId);
        return new ArrayList<>(buildingsMap.values());
    }

    

    private void addDirectRoomAccessBuildings(Long userId, Map<Long, BuildingWithRoomsDTO> buildingsMap) {
        String sql ="SELECT DISTINCT b.id AS building_id, b.name AS building_name, b.description AS building_desc, mo.id AS room_id, mo.room_id AS room_code, mo.name AS room_name, mo.description AS room_desc, mo.category, mo.room_type, mo.contact_details, mo.accessible, mo.entrance_node_id, f.id AS floor_id, f.floor_number, f.display_name AS floor_name FROM user_room_permissions urp JOIN map_objects mo ON urp.map_object_id = mo.id JOIN floors f ON mo.floor_id = f.id JOIN buildings b ON f.building_id = b.id WHERE urp.userid = ?1 AND mo.object_type = 'ROOM'";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, userId);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        for (Object[] row : results) {
            int col = 0;
            Long buildingId = ((Number) row[col++]).longValue();
            String buildingName = (String) row[col++];
            String buildingDesc = (String) row[col++];
            Long roomId = ((Number) row[col++]).longValue();
            String roomCode = (String) row[col++];
            String roomName = (String) row[col++];
            String roomDesc = (String) row[col++];
            String category = (String) row[col++];
            String roomType = (String) row[col++];
            String contactDetails = (String) row[col++];
            Boolean accessible = (Boolean) row[col++];
            Object entranceNodeObj = row[col++];
            Long entranceNodeId = entranceNodeObj != null ? ((Number) entranceNodeObj).longValue() : null;
            Long floorId = ((Number) row[col++]).longValue();
            Integer floorNumber = ((Number) row[col++]).intValue();
            String floorName = (String) row[col++];

            BuildingWithRoomsDTO building = buildingsMap.computeIfAbsent(
                    buildingId,
                    id -> new BuildingWithRoomsDTO(
                            id,
                            buildingName != null ? buildingName : "",
                            buildingDesc != null ? buildingDesc : "",
                            new ArrayList<>()
                    )
            );

            boolean roomExists = building.getRooms().stream()
                    .anyMatch(r -> roomId.equals(r.getId()));

            if (!roomExists) {
                RoomDTO room = new RoomDTO(
                        roomId,
                        roomCode != null ? roomCode : "",
                        roomName != null ? roomName : "",
                        roomDesc != null ? roomDesc : "",
                        floorId,
                        floorNumber,
                        floorName != null ? floorName : "",
                        category != null ? category : "",
                        roomType != null ? roomType : "",
                        contactDetails != null ? contactDetails : "",
                        accessible != null ? accessible : false,
                        entranceNodeId
                );

                building.getRooms().add(room);
            }
        }
    }
    

    private RoomDTO convertToRoomDTO(MapObject room) {
        RoomDTO dto = new RoomDTO();
        dto.setRoomId(room.getRoomId() != null ? room.getRoomId() : "");
        dto.setId(room.getId());
        dto.setName(room.getName() != null ? room.getName() : "");
        dto.setDescription(room.getDescription() != null ? room.getDescription() : "");
        dto.setFloorId(room.getFloor().getId());
        dto.setFloorNumber(room.getFloor().getFloorNumber());
        dto.setFloorName(room.getFloor().getDisplayName() != null ? room.getFloor().getDisplayName() : "");
        dto.setCategory(room.getCategory() != null ? room.getCategory() : "");
        dto.setRoomType(room.getRoomType() != null ? room.getRoomType() : "");
        dto.setContactDetails(room.getContactDetails() != null ? room.getContactDetails() : "");
        dto.setAccessible(room.isAccessible());

       

        if (room.getEntranceNode() != null) {
            dto.setEntranceNodeId(room.getEntranceNode().getId());
        } else {
            dto.setEntranceNodeId(null);
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public List<BuildingWithFloorsAndRoomsDTO> getUserInaccessibleBuildingsWithRooms(Long userId) {
        logger.debug("Retrieving inaccessible buildings and rooms for user {}", userId);

       

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

       

        String sql = "WITH " +
                "user_accessible_buildings AS ( " +
                "   SELECT DISTINCT buildingid FROM user_building_permissions WHERE userid = ?1 " +
                "), " +
                "user_accessible_rooms AS ( " +
                "   SELECT DISTINCT map_object_id FROM user_room_permissions WHERE userid = ?1 " +
                ") " +
                "SELECT " +
                "   b.id AS building_id, " +
                "   b.name AS building_name, " +
                "   b.description AS building_desc, " +
                "   f.id AS floor_id, " +
                "   f.floor_number, " +
                "   f.display_name AS floor_name, " +
                "   mo.id AS room_id, " +
                "   mo.room_id AS room_code, " +
                "   mo.name AS room_name, " +
                "   mo.description AS room_desc " +
                "FROM " +
                "   buildings b " +
                "JOIN floors f ON f.building_id = b.id " +
                "JOIN map_objects mo ON mo.floor_id = f.id " +
                "WHERE " +
                "   mo.object_type = 'ROOM' " +
                "   AND b.id NOT IN (SELECT buildingid FROM user_accessible_buildings) " +
                "   AND mo.id NOT IN (SELECT map_object_id FROM user_accessible_rooms) " +
                "ORDER BY b.name, f.floor_number, mo.name";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, userId);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        Map<Long, BuildingWithFloorsAndRoomsDTO> buildingsMap = new HashMap<>();

        for (Object[] row : results) {
            int col = 0;
            Long buildingId = ((Number) row[col++]).longValue();
            String buildingName = (String) row[col++];
            String buildingDesc = (String) row[col++];
            Long floorId = ((Number) row[col++]).longValue();
            Integer floorNumber = ((Number) row[col++]).intValue();
            String floorName = (String) row[col++];
            Long roomId = ((Number) row[col++]).longValue();
            String roomCode = (String) row[col++];
            String roomName = (String) row[col++];
            String roomDesc = (String) row[col];

           

            BuildingWithFloorsAndRoomsDTO building = buildingsMap.computeIfAbsent(
                    buildingId,
                    id -> new BuildingWithFloorsAndRoomsDTO(
                            id,
                            buildingName != null ? buildingName : "",
                            buildingDesc != null ? buildingDesc : "",
                            new ArrayList<>()
                    )
            );

           

            FloorWithRoomsDTO floor = building.getFloors().stream()
                    .filter(f -> f.getFloorId().equals(floorId))
                    .findFirst()
                    .orElseGet(() -> {
                        FloorWithRoomsDTO newFloor = new FloorWithRoomsDTO(
                                floorId,
                                floorNumber,
                                floorName != null ? floorName : "",
                                new ArrayList<>()
                        );
                        building.getFloors().add(newFloor);
                        return newFloor;
                    });

           

            floor.getRooms().add(new SimpleRoomDTO(
                    roomId,
                    roomCode != null ? roomCode : "",
                    roomName != null ? roomName : "",
                    roomDesc != null ? roomDesc : ""
            ));
        }

        logger.debug("Found {} inaccessible buildings with rooms for user {}", buildingsMap.size(), userId);
        return new ArrayList<>(buildingsMap.values());
    }
}