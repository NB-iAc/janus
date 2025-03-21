package dev.thesis.janus.central.controller;

import dev.thesis.janus.central.dto.MapObjectCreateDTO;
import dev.thesis.janus.central.dto.MapObjectDTO;
import dev.thesis.janus.central.dto.RoomUpdateDTO;
import dev.thesis.janus.central.model.MapObjectType;
import dev.thesis.janus.central.service.MapObjectService;
import dev.thesis.janus.central.service.UserPermissionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/map-objects")
@RequiredArgsConstructor
@Api(tags = "Map Object Management")
public class MapObjectController {

    private final MapObjectService mapObjectService;
    private final UserPermissionService userPermissionService;

    @GetMapping
    @ApiOperation("Get all map objects")
    public ResponseEntity<List<MapObjectDTO>> getAllMapObjects() {
        List<MapObjectDTO> mapObjects = mapObjectService.getAllMapObjects();
        return ResponseEntity.ok(mapObjects);
    }

    @GetMapping("/floor/{floorId}")
    @ApiOperation("Get map objects by floor ID")
    public ResponseEntity<List<MapObjectDTO>> getMapObjectsByFloorId(@PathVariable Long floorId) {
        List<MapObjectDTO> mapObjects = mapObjectService.getMapObjectsByFloorId(floorId);
        return ResponseEntity.ok(mapObjects);
    }

    @GetMapping("/{id}")
    @ApiOperation("Get a map object by ID")
    public ResponseEntity<MapObjectDTO> getMapObjectById(@PathVariable Long id) {
        MapObjectDTO mapObject = mapObjectService.getMapObjectById(id);
        return ResponseEntity.ok(mapObject);
    }

    @GetMapping("/room/{roomId}")
    @ApiOperation("Get a map object by room ID")
    public ResponseEntity<MapObjectDTO> getMapObjectByRoomId(@PathVariable String roomId) {
        MapObjectDTO mapObject = mapObjectService.getMapObjectByRoomId(roomId);
        return ResponseEntity.ok(mapObject);
    }

    @PostMapping
    @ApiOperation("Create a new map object")
    public ResponseEntity<MapObjectDTO> createMapObject(@Valid @RequestBody MapObjectCreateDTO mapObjectDTO) {
        MapObjectDTO createdMapObject = mapObjectService.createMapObject(mapObjectDTO);
        return new ResponseEntity<>(createdMapObject, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @ApiOperation("Update an existing map object")
    public ResponseEntity<MapObjectDTO> updateMapObject(
            @PathVariable Long id,
            @Valid @RequestBody MapObjectCreateDTO mapObjectDTO) {
        MapObjectDTO updatedMapObject = mapObjectService.updateMapObject(id, mapObjectDTO);
        return ResponseEntity.ok(updatedMapObject);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("Delete a map object")
    public ResponseEntity<Void> deleteMapObject(@PathVariable Long id) {
        mapObjectService.deleteMapObject(id);
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/{id}/update-room")
    @ApiOperation("Update room details with permission validation")
    public ResponseEntity<MapObjectDTO> updateRoomWithPermissionCheck(
            @PathVariable Long id,
            @Valid @RequestBody RoomUpdateDTO roomUpdateDTO,
            @RequestAttribute(name = "userId", required = false) Long userId) {

       

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

       

        if (!userPermissionService.canUserAccessMapObject(userId, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

       

        MapObjectDTO existingMapObject = mapObjectService.getMapObjectById(id);
        if (existingMapObject.getObjectType() != MapObjectType.ROOM) {
            return ResponseEntity.badRequest().build();
        }

       

        MapObjectDTO updatedMapObject = mapObjectService.updateRoomBasicDetails(
                id,
                roomUpdateDTO.getName(),
                roomUpdateDTO.getDescription(),
                roomUpdateDTO.getContactDetails(),
                roomUpdateDTO.getRoomType() 

        );

        return ResponseEntity.ok(updatedMapObject);
    }
}
