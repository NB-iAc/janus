package dev.thesis.janus.central.controller;

import dev.thesis.janus.central.dto.FloorCreateDTO;
import dev.thesis.janus.central.dto.FloorDTO;
import dev.thesis.janus.central.service.FloorService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/floors")
@RequiredArgsConstructor
@Api(tags = "Floor Management")
public class FloorController {

    private final FloorService floorService;
    @GetMapping
    @ApiOperation("Get all floors")
    public ResponseEntity<List<FloorDTO>> getAllFloors() {
        List<FloorDTO> floors = floorService.getAllFloors();
        return ResponseEntity.ok(floors);
    }

    @GetMapping("/building/{buildingId}")
    @ApiOperation("Get floors by building ID")
    public ResponseEntity<List<FloorDTO>> getFloorsByBuildingId(@PathVariable Long buildingId) {
        List<FloorDTO> floors = floorService.getFloorsByBuildingId(buildingId);
        return ResponseEntity.ok(floors);
    }

    @GetMapping("/{id}")
    @ApiOperation("Get a floor by ID")
    public ResponseEntity<FloorDTO> getFloorById(@PathVariable Long id) {
        FloorDTO floor = floorService.getFloorById(id);
        return ResponseEntity.ok(floor);
    }
    @PutMapping("/name/{buildingId}/{floorNumber}/{newName}")
    @ApiOperation("Update floor name")
    public ResponseEntity<FloorDTO> updateFloorName(
            @PathVariable Long buildingId,
            @PathVariable Integer floorNumber,
            @PathVariable String newName) {
        FloorDTO updatedFloor = floorService.updateFloorName(buildingId, floorNumber, newName);
        return ResponseEntity.ok(updatedFloor);
    }
    @PostMapping
    @ApiOperation("Create a new floor")
    public ResponseEntity<FloorDTO> createFloor(@Valid @RequestBody FloorCreateDTO floorDTO) {
        FloorDTO createdFloor = floorService.createFloor(floorDTO);
        return new ResponseEntity<>(createdFloor, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @ApiOperation("Update an existing floor")
    public ResponseEntity<FloorDTO> updateFloor(
            @PathVariable Long id,
            @Valid @RequestBody FloorCreateDTO floorDTO) {
        FloorDTO updatedFloor = floorService.updateFloor(id, floorDTO);
        return ResponseEntity.ok(updatedFloor);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("Delete a floor")
    public ResponseEntity<Void> deleteFloor(@PathVariable Long id) {
        floorService.deleteFloor(id);
        return ResponseEntity.noContent().build();
    }
}
