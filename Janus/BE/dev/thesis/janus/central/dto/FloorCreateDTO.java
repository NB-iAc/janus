package dev.thesis.janus.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FloorCreateDTO {
    @NotNull(message = "Building ID is required")
    private Long buildingId;

    @NotNull(message = "Floor number is required")
    private Integer floorNumber;

    private String displayName;

    private boolean accessible = true;
}
