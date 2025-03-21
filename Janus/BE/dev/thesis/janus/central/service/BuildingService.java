package dev.thesis.janus.central.service;

import dev.thesis.janus.central.dto.*;
import dev.thesis.janus.central.exception.ResourceNotFoundException;
import dev.thesis.janus.central.model.*;
import dev.thesis.janus.central.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuildingService {
    private final BuildingLogService buildingLogService;
    private final BuildingRepository buildingRepository;
    private final UserRepository userRepository;
    private final UserBuildingPermissionsRepository userBuildingPermissionsRepository;
    private final FloorRepository floorRepository;
    private final MapObjectRepository mapObjectRepository;
    @Transactional(readOnly = true)
    public List<BuildingDTO> getAllBuildings() {
        return buildingRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BuildingDTO getBuildingById(Long id) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + id));
        return convertToDTO(building);
    }
    @Transactional(readOnly = true)
    public BuildingElevationDTO getBuildingElevationMap(Long buildingId) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + buildingId));

        List<Floor> floors = floorRepository.findByBuildingId(buildingId);

        List<FloorElevationDTO> floorDTOs = floors.stream()
                .map(floor -> {
                   

                    List<MapObject> elevationObjects = mapObjectRepository.findByFloorIdAndObjectTypeIn(
                            floor.getId(),
                            Arrays.asList(MapObjectType.ELEVATOR, MapObjectType.STAIR, MapObjectType.ESCALATOR)
                    );

                   

                    List<ElevationMapObjectDTO> elevationObjectDTOs = elevationObjects.stream()
                            .map(obj -> new ElevationMapObjectDTO(
                                    obj.getId(),
                                    obj.getName(),
                                    obj.getObjectType(),
                                    obj.getEntranceNode() != null ? obj.getEntranceNode().getId() : null
                            ))
                            .collect(Collectors.toList());

                   

                    return new FloorElevationDTO(
                            floor.getId(),
                            floor.getFloorNumber(),
                            floor.getDisplayName(),
                            elevationObjectDTOs
                    );
                })
                .sorted(Comparator.comparing(FloorElevationDTO::getFloorNumber))
                .collect(Collectors.toList());

        return new BuildingElevationDTO(
                building.getId(),
                building.getName(),
                floorDTOs
        );
    }
    @Transactional
    public BuildingDTO createBuilding(BuildingCreateDTO buildingCreateDTO, Long creatorUserId) {
       

        Building building = new Building();
        building.setName(buildingCreateDTO.getName());
        building.setDescription(buildingCreateDTO.getDescription());

        Building savedBuilding = buildingRepository.save(building);

       

        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + creatorUserId));

        UserBuildingPermissions permission = new UserBuildingPermissions();
        permission.setUser(creator);
        permission.setBuilding(savedBuilding);
        permission.setAllAccess(true);
        userBuildingPermissionsRepository.save(permission);
        buildingLogService.createBuildingLog(
                new CreateBuildingLogDTO(
                        creatorUserId,
                        savedBuilding.getId(),
                        "CREATE_BUILDING",
                        savedBuilding.getName(),
                        null,
                        "Building created"
                )
        );

        return convertToDTO(savedBuilding);
    }

    @Transactional
    public BuildingDTO updateBuilding(Long id, BuildingCreateDTO buildingCreateDTO) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + id));

        building.setName(buildingCreateDTO.getName());
        building.setDescription(buildingCreateDTO.getDescription());

        Building updatedBuilding = buildingRepository.save(building);
        return convertToDTO(updatedBuilding);
    }

    @Transactional
    public void deleteBuilding(Long id) {
        if (!buildingRepository.existsById(id)) {
            throw new ResourceNotFoundException("Building not found with id: " + id);
        }
        buildingRepository.deleteById(id);
    }

    private BuildingDTO convertToDTO(Building building) {
        return new BuildingDTO(
                building.getId(),
                building.getName(),
                building.getDescription(),
                building.getCreatedAt(),
                building.getUpdatedAt()
        );
    }
    @Transactional
    public BuildingDTO createBuilding(BuildingCreateDTO buildingCreateDTO) {
        Building building = new Building();
        building.setName(buildingCreateDTO.getName());
        building.setDescription(buildingCreateDTO.getDescription());

        Building savedBuilding = buildingRepository.save(building);

        return convertToDTO(savedBuilding);
    }
}
