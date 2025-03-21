package dev.thesis.janus.central.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.thesis.janus.central.dto.BuildingDataDTO;
import dev.thesis.janus.central.service.BuildingDataService;

@RestController
@RequestMapping("/building-data")
@RequiredArgsConstructor
@Api(tags = "Building Data Management")
public class BuildingDataController {

    private final BuildingDataService buildingDataService;

    @GetMapping("/{buildingId}")
    @ApiOperation("Get all data for a building (floors, map objects, nodes, connections)")
    public ResponseEntity<BuildingDataDTO> getBuildingData(@PathVariable Long buildingId) {
        BuildingDataDTO buildingData = buildingDataService.getBuildingData(buildingId);
        return ResponseEntity.ok(buildingData);
    }

    @PostMapping
    @ApiOperation("Upload all data for a building in bulk")
    public ResponseEntity<BuildingDataDTO> uploadBuildingData(@RequestBody BuildingDataDTO buildingData) {
        BuildingDataDTO uploadedData = buildingDataService.uploadBuildingData(buildingData);
        return new ResponseEntity<>(uploadedData, HttpStatus.CREATED);
    }
}