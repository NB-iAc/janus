package dev.thesis.janus.central.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeCreateDTO {
    @NotNull(message = "X coordinate is required")
    private Float x;

    @NotNull(message = "Y coordinate is required")
    private Float y;

    @NotNull(message = "Floor ID is required")
    private Long floorId;

    private boolean isElevationNode = false;

    private String nodeType;
}