package dev.thesis.janus.central.dto;

import dev.thesis.janus.central.model.MapObjectType;
import dev.thesis.janus.central.model.RoomCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapObjectDTO {
    private Long id;
    private Long floorId;
    private MapObjectType objectType;
    private String name;
    private String roomId;
    private String category;
    private String contactDetails;
    private String roomType;
    private String description;
    private Long entranceNodeId;
    private List<PointDTO> points;
    private boolean accessible;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}