package dev.thesis.janus.central.controller;

import dev.thesis.janus.central.dto.NodeConnectionDTO;
import dev.thesis.janus.central.dto.NodeCreateDTO;
import dev.thesis.janus.central.dto.NodeDTO;
import dev.thesis.janus.central.service.NodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/nodes")
@RequiredArgsConstructor
@Api(tags = "Navigation Node Management")
public class NodeController {

    private final NodeService nodeService;

    @GetMapping
    @ApiOperation("Get all nodes")
    public ResponseEntity<List<NodeDTO>> getAllNodes() {
        List<NodeDTO> nodes = nodeService.getAllNodes();
        return ResponseEntity.ok(nodes);
    }

    @GetMapping("/floor/{floorId}")
    @ApiOperation("Get nodes by floor ID")
    public ResponseEntity<List<NodeDTO>> getNodesByFloorId(@PathVariable Long floorId) {
        List<NodeDTO> nodes = nodeService.getNodesByFloorId(floorId);
        return ResponseEntity.ok(nodes);
    }

    @GetMapping("/floor/{floorId}/elevation")
    @ApiOperation("Get elevation nodes (stairs, elevators) by floor ID")
    public ResponseEntity<List<NodeDTO>> getElevationNodesByFloorId(@PathVariable Long floorId) {
        List<NodeDTO> nodes = nodeService.getElevationNodesByFloorId(floorId);
        return ResponseEntity.ok(nodes);
    }

    @GetMapping("/building/{buildingId}")
    @ApiOperation("Get all nodes in a building")
    public ResponseEntity<List<NodeDTO>> getNodesByBuildingId(@PathVariable Long buildingId) {
        List<NodeDTO> nodes = nodeService.getNodesByBuildingId(buildingId);
        return ResponseEntity.ok(nodes);
    }

    @GetMapping("/{id}")
    @ApiOperation("Get a node by ID")
    public ResponseEntity<NodeDTO> getNodeById(@PathVariable Long id) {
        NodeDTO node = nodeService.getNodeById(id);
        return ResponseEntity.ok(node);
    }

    @PostMapping
    @ApiOperation("Create a new node")
    public ResponseEntity<NodeDTO> createNode(@Valid @RequestBody NodeCreateDTO nodeDTO) {
        NodeDTO createdNode = nodeService.createNode(nodeDTO);
        return new ResponseEntity<>(createdNode, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @ApiOperation("Update an existing node")
    public ResponseEntity<NodeDTO> updateNode(
            @PathVariable Long id,
            @Valid @RequestBody NodeCreateDTO nodeDTO) {
        NodeDTO updatedNode = nodeService.updateNode(id, nodeDTO);
        return ResponseEntity.ok(updatedNode);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("Delete a node")
    public ResponseEntity<Void> deleteNode(@PathVariable Long id) {
        nodeService.deleteNode(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/connections")
    @ApiOperation("Create a connection between nodes")
    public ResponseEntity<NodeConnectionDTO> createNodeConnection(@Valid @RequestBody NodeConnectionDTO connectionDTO) {
        NodeConnectionDTO createdConnection = nodeService.createNodeConnection(connectionDTO);
        return new ResponseEntity<>(createdConnection, HttpStatus.CREATED);
    }

    @GetMapping("/connections/building/{buildingId}")
    @ApiOperation("Get all node connections in a building")
    public ResponseEntity<List<NodeConnectionDTO>> getNodeConnectionsByBuildingId(@PathVariable Long buildingId) {
        List<NodeConnectionDTO> connections = nodeService.getNodeConnectionsByBuildingId(buildingId);
        return ResponseEntity.ok(connections);
    }

    @DeleteMapping("/connections")
    @ApiOperation("Delete a connection between nodes")
    public ResponseEntity<Void> deleteNodeConnection(
            @RequestParam Long sourceNodeId,
            @RequestParam Long targetNodeId) {
        nodeService.deleteNodeConnection(sourceNodeId, targetNodeId);
        return ResponseEntity.noContent().build();
    }
}