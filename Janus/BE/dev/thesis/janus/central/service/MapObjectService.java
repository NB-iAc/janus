package dev.thesis.janus.central.service;

import dev.thesis.janus.central.dto.CreateBuildingLogDTO;
import dev.thesis.janus.central.dto.MapObjectCreateDTO;
import dev.thesis.janus.central.dto.MapObjectDTO;
import dev.thesis.janus.central.dto.PointDTO;
import dev.thesis.janus.central.exception.ResourceNotFoundException;
import dev.thesis.janus.central.model.Floor;
import dev.thesis.janus.central.model.MapObject;
import dev.thesis.janus.central.model.MapObjectPoint;
import dev.thesis.janus.central.model.Node;
import dev.thesis.janus.central.repository.FloorRepository;
import dev.thesis.janus.central.repository.MapObjectPointRepository;
import dev.thesis.janus.central.repository.MapObjectRepository;
import dev.thesis.janus.central.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class MapObjectService {
    private final MapObjectRepository mapObjectRepository;
    private final MapObjectPointRepository mapObjectPointRepository;
    private final FloorRepository floorRepository;
    private final NodeRepository nodeRepository;
    @Transactional
    public MapObjectDTO updateRoomBasicDetails(Long id, String name, String description, String contactDetails, String roomType) {
        MapObject mapObject = mapObjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Map object not found with id: " + id));

       

        mapObject.setName(name);
        mapObject.setDescription(description);
        mapObject.setContactDetails(contactDetails);
        mapObject.setCategory(roomType != null ? roomType : "");


       

        MapObject updatedMapObject = mapObjectRepository.save(mapObject);
        return convertToDTO(updatedMapObject);
    }
    @Transactional
    public MapObjectDTO createMapObject(MapObjectCreateDTO mapObjectCreateDTO) {
        Floor floor = floorRepository.findById(mapObjectCreateDTO.getFloorId())
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found with id: " + mapObjectCreateDTO.getFloorId()));

        MapObject mapObject = new MapObject();
        mapObject.setFloor(floor);
        mapObject.setObjectType(mapObjectCreateDTO.getObjectType());
        mapObject.setName(mapObjectCreateDTO.getName());
        mapObject.setRoomId(mapObjectCreateDTO.getRoomId());
        mapObject.setCategory(mapObjectCreateDTO.getCategory());

        mapObject.setContactDetails(mapObjectCreateDTO.getContactDetails());
        mapObject.setRoomType(mapObjectCreateDTO.getRoomType());
        mapObject.setDescription(mapObjectCreateDTO.getDescription());
        mapObject.setAccessible(mapObjectCreateDTO.isAccessible());

       

        if (mapObjectCreateDTO.getEntranceNodeId() != null) {
            Node entranceNode = nodeRepository.findById(mapObjectCreateDTO.getEntranceNodeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Node not found with id: " + mapObjectCreateDTO.getEntranceNodeId()));
            mapObject.setEntranceNode(entranceNode);
        }

       

        MapObject savedMapObject = mapObjectRepository.save(mapObject);

       

        List<MapObjectPoint> points = IntStream.range(0, mapObjectCreateDTO.getPoints().size())
                .mapToObj(i -> {
                    PointDTO pointDTO = mapObjectCreateDTO.getPoints().get(i);
                    MapObjectPoint point = new MapObjectPoint();
                    point.setMapObject(savedMapObject);
                    point.setX(pointDTO.getX());
                    point.setY(pointDTO.getY());
                    point.setPointOrder(i);
                    return point;
                })
                .collect(Collectors.toList());

        mapObjectPointRepository.saveAll(points);

       

        MapObject mapObjectWithPoints = mapObjectRepository.findById(savedMapObject.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Map object not found after saving"));

        return convertToDTO(mapObjectWithPoints);
    }

    @Transactional
    public MapObjectDTO updateMapObject(Long id, MapObjectCreateDTO mapObjectCreateDTO) {
        MapObject mapObject = mapObjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Map object not found with id: " + id));

        Floor floor = floorRepository.findById(mapObjectCreateDTO.getFloorId())
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found with id: " + mapObjectCreateDTO.getFloorId()));

        mapObject.setFloor(floor);
        mapObject.setObjectType(mapObjectCreateDTO.getObjectType());
        mapObject.setName(mapObjectCreateDTO.getName());
        mapObject.setRoomId(mapObjectCreateDTO.getRoomId());
        mapObject.setCategory(mapObjectCreateDTO.getCategory());

        mapObject.setContactDetails(mapObjectCreateDTO.getContactDetails());
        mapObject.setRoomType(mapObjectCreateDTO.getRoomType());
        mapObject.setDescription(mapObjectCreateDTO.getDescription());
        mapObject.setAccessible(mapObjectCreateDTO.isAccessible());

       

        if (mapObjectCreateDTO.getEntranceNodeId() != null) {
            Node entranceNode = nodeRepository.findById(mapObjectCreateDTO.getEntranceNodeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Node not found with id: " + mapObjectCreateDTO.getEntranceNodeId()));
            mapObject.setEntranceNode(entranceNode);
        } else {
            mapObject.setEntranceNode(null);
        }

       

        mapObjectPointRepository.deleteByMapObjectId(mapObject.getId());

        List<MapObjectPoint> points = IntStream.range(0, mapObjectCreateDTO.getPoints().size())
                .mapToObj(i -> {
                    PointDTO pointDTO = mapObjectCreateDTO.getPoints().get(i);
                    MapObjectPoint point = new MapObjectPoint();
                    point.setMapObject(mapObject);
                    point.setX(pointDTO.getX());
                    point.setY(pointDTO.getY());
                    point.setPointOrder(i);
                    return point;
                })
                .collect(Collectors.toList());

        mapObjectPointRepository.saveAll(points);

       

        MapObject updatedMapObject = mapObjectRepository.save(mapObject);

        return convertToDTO(updatedMapObject);
    }
    @Transactional(readOnly = true)
    public List<MapObjectDTO> getAllMapObjects() {
        return mapObjectRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MapObjectDTO> getMapObjectsByFloorId(Long floorId) {
        return mapObjectRepository.findByFloorId(floorId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MapObjectDTO getMapObjectById(Long id) {
        MapObject mapObject = mapObjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Map object not found with id: " + id));
        return convertToDTO(mapObject);
    }

    @Transactional(readOnly = true)
    public MapObjectDTO getMapObjectByRoomId(String roomId) {
        MapObject mapObject = mapObjectRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Map object not found with roomId: " + roomId));
        return convertToDTO(mapObject);
    }

    @Transactional
    public void deleteMapObject(Long id) {
        if (!mapObjectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Map object not found with id: " + id);
        }
        mapObjectRepository.deleteById(id);
    }

    MapObjectDTO convertToDTO(MapObject mapObject) {
        List<PointDTO> pointDTOs = mapObjectPointRepository.findByMapObjectOrderByPointOrder(mapObject)
                .stream()
                .map(point -> new PointDTO(point.getX(), point.getY()))
                .collect(Collectors.toList());

        return new MapObjectDTO(
                mapObject.getId(),
                mapObject.getFloor().getId(),
                mapObject.getObjectType(),
                mapObject.getName(),
                mapObject.getRoomId(),
                mapObject.getCategory(),
                mapObject.getContactDetails(),
                mapObject.getRoomType(),
                mapObject.getDescription(),
                mapObject.getEntranceNode() != null ? mapObject.getEntranceNode().getId() : null,
                pointDTOs,
                mapObject.isAccessible(),
                mapObject.getCreatedAt(),
                mapObject.getUpdatedAt()
        );
    }
}