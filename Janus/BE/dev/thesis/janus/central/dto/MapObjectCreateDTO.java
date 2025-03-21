package dev.thesis.janus.central.dto;

import dev.thesis.janus.central.model.MapObjectType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapObjectCreateDTO {
    @NotNull(message = "Floor ID is required")
    private Long floorId;

    @NotNull(message = "Object type is required")
    private MapObjectType objectType;

    private String name;

    private String roomId;

    private String category = "DEFAULT";

    private String contactDetails;

    private String roomType;

    private String description;

    private Long entranceNodeId;

    @NotEmpty(message = "At least one point is required")
    private List<PointDTO> points;

    private boolean accessible = true;
}