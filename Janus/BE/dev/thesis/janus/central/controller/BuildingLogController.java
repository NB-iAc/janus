package dev.thesis.janus.central.controller;

import dev.thesis.janus.central.dto.CreateBuildingLogDTO;
import dev.thesis.janus.central.dto.BuildingLogDTO;
import dev.thesis.janus.central.service.BuildingLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/building-logs")
@RequiredArgsConstructor
@Api(tags = "Building Logs")
public class BuildingLogController {

    private final BuildingLogService buildingLogService;

    @PostMapping
    @ApiOperation("Record a log about a building or floor change")
    public ResponseEntity<BuildingLogDTO> createBuildingLog(@Valid @RequestBody CreateBuildingLogDTO logDTO) {
        BuildingLogDTO createdLog = buildingLogService.createBuildingLog(logDTO);
        return new ResponseEntity<>(createdLog, HttpStatus.CREATED);
    }

    @GetMapping("/building/{buildingId}")
    @ApiOperation("Get all logs for a building in chronological order")
    public ResponseEntity<List<BuildingLogDTO>> getBuildingLogs(@PathVariable Long buildingId) {
        List<BuildingLogDTO> logs = buildingLogService.getBuildingLogs(buildingId);
        return ResponseEntity.ok(logs);
    }
}