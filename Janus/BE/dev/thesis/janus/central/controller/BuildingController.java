package dev.thesis.janus.central.controller;

import dev.thesis.janus.central.dto.BuildingCreateDTO;
import dev.thesis.janus.central.dto.BuildingDTO;
import dev.thesis.janus.central.dto.BuildingElevationDTO;
import dev.thesis.janus.central.dto.UserDTO;
import dev.thesis.janus.central.service.BuildingAdminService;
import dev.thesis.janus.central.service.BuildingDeletionService;
import dev.thesis.janus.central.service.BuildingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/buildings")
@RequiredArgsConstructor
@Api(tags = "Building Management")
public class BuildingController {
    private final BuildingAdminService buildingAdminService;

    @GetMapping("/{id}/admins")
    @ApiOperation("Get administrators for a building")
    public ResponseEntity<List<UserDTO>> getBuildingAdmins(@PathVariable Long id) {
        logger.info("Getting administrators for building ID: {}", id);
        List<UserDTO> admins = buildingAdminService.getBuildingAdministrators(id);
        return ResponseEntity.ok(admins);
    }
    private final BuildingDeletionService buildingDeletionService;

    private final BuildingService buildingService;
    private static final Logger logger = LogManager.getLogger(BuildingController.class);
    @GetMapping("/{id}/elevation-map")
    @ApiOperation("Get a building's elevation map objects (stairs, elevators, etc.) organized by floor")
    public ResponseEntity<BuildingElevationDTO> getBuildingElevationMap(@PathVariable Long id) {
        BuildingElevationDTO elevationMap = buildingService.getBuildingElevationMap(id);
        return ResponseEntity.ok(elevationMap);
    }

    @GetMapping
    @ApiOperation("Get all buildings")
    public ResponseEntity<List<BuildingDTO>> getAllBuildings() {
        logger.error("User requested all buildings");
        System.out.println("User requested all buildings");
        List<BuildingDTO> buildings = buildingService.getAllBuildings();
        return ResponseEntity.ok(buildings);
    }

    @GetMapping("/{id}")
    @ApiOperation("Get a building by ID")
    public ResponseEntity<BuildingDTO> getBuildingById(@PathVariable Long id) {
        BuildingDTO building = buildingService.getBuildingById(id);
        return ResponseEntity.ok(building);
    }

    @PostMapping
    @ApiOperation("Create a new building")
    public ResponseEntity<BuildingDTO> createBuilding(
            @Valid @RequestBody BuildingCreateDTO buildingDTO,
            @RequestAttribute(name = "userId", required = true) Long userId) {
        BuildingDTO createdBuilding = buildingService.createBuilding(buildingDTO, userId);
        return new ResponseEntity<>(createdBuilding, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @ApiOperation("Update an existing building")
    public ResponseEntity<BuildingDTO> updateBuilding(
            @PathVariable Long id,
            @Valid @RequestBody BuildingCreateDTO buildingDTO) {
        BuildingDTO updatedBuilding = buildingService.updateBuilding(id, buildingDTO);
        return ResponseEntity.ok(updatedBuilding);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("Delete a building")
    public ResponseEntity<Void> deleteBuilding(@PathVariable Long id) {
        buildingDeletionService.deleteBuildingEfficiently(id);
        return ResponseEntity.noContent().build();
    }
}
