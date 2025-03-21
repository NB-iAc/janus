package dev.thesis.janus.central.service;

import dev.thesis.janus.central.dto.FloorCreateDTO;
import dev.thesis.janus.central.dto.FloorDTO;
import dev.thesis.janus.central.exception.ResourceNotFoundException;
import dev.thesis.janus.central.model.Building;
import dev.thesis.janus.central.model.Floor;
import dev.thesis.janus.central.repository.BuildingRepository;
import dev.thesis.janus.central.repository.FloorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FloorService {

    private final BuildingLogService buildingLogService;
    private final FloorRepository floorRepository;
    private final BuildingRepository buildingRepository;

    @Transactional(readOnly = true)
    public List<FloorDTO> getAllFloors() {
        return floorRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FloorDTO> getFloorsByBuildingId(Long buildingId) {
        return floorRepository.findByBuildingId(buildingId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    @Transactional
    public FloorDTO updateFloorName(Long buildingId, Integer floorNumber, String newName) {
        Floor floor = floorRepository.findByBuildingIdAndFloorNumber(buildingId, floorNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Floor not found with buildingId: " + buildingId + " and floorNumber: " + floorNumber));

        floor.setDisplayName(newName);
        Floor updatedFloor = floorRepository.save(floor);
        return convertToDTO(updatedFloor);
    }
    @Transactional(readOnly = true)
    public FloorDTO getFloorById(Long id) {
        Floor floor = floorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found with id: " + id));
        return convertToDTO(floor);
    }

    @Transactional
    public FloorDTO createFloor(FloorCreateDTO floorCreateDTO) {
        Building building = buildingRepository.findById(floorCreateDTO.getBuildingId())
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + floorCreateDTO.getBuildingId()));

       

        floorRepository.findByBuildingIdAndFloorNumber(building.getId(), floorCreateDTO.getFloorNumber())
                .ifPresent(existingFloor -> {
                    throw new IllegalArgumentException("Floor " + floorCreateDTO.getFloorNumber() +
                            " already exists in building " + building.getName());
                });

        Floor floor = new Floor();
        floor.setBuilding(building);
        floor.setFloorNumber(floorCreateDTO.getFloorNumber());
        floor.setDisplayName(floorCreateDTO.getDisplayName() != null ?
                floorCreateDTO.getDisplayName() : "Floor " + floorCreateDTO.getFloorNumber());
        floor.setAccessible(floorCreateDTO.isAccessible());

        Floor savedFloor = floorRepository.save(floor);
        return convertToDTO(savedFloor);
    }

    @Transactional
    public FloorDTO updateFloor(Long id, FloorCreateDTO floorCreateDTO) {
        Floor floor = floorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found with id: " + id));

        Building building = buildingRepository.findById(floorCreateDTO.getBuildingId())
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + floorCreateDTO.getBuildingId()));

       

        if (!floor.getFloorNumber().equals(floorCreateDTO.getFloorNumber())) {
            floorRepository.findByBuildingIdAndFloorNumber(building.getId(), floorCreateDTO.getFloorNumber())
                    .ifPresent(existingFloor -> {
                        throw new IllegalArgumentException("Floor " + floorCreateDTO.getFloorNumber() +
                                " already exists in building " + building.getName());
                    });
        }

        floor.setBuilding(building);
        floor.setFloorNumber(floorCreateDTO.getFloorNumber());
        floor.setDisplayName(floorCreateDTO.getDisplayName() != null ?
                floorCreateDTO.getDisplayName() : "Floor " + floorCreateDTO.getFloorNumber());
        floor.setAccessible(floorCreateDTO.isAccessible());

        Floor updatedFloor = floorRepository.save(floor);
        return convertToDTO(updatedFloor);
    }

    @Transactional
    public void deleteFloor(Long id) {
        if (!floorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Floor not found with id: " + id);
        }
        floorRepository.deleteById(id);
    }

    private FloorDTO convertToDTO(Floor floor) {
        return new FloorDTO(
                floor.getId(),
                floor.getBuilding().getId(),
                floor.getFloorNumber(),
                floor.getDisplayName(),
                floor.isAccessible()
        );
    }
}
