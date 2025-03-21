package dev.thesis.janus.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBuildingLogDTO {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Building ID is required")
    private Long buildingId;

    @NotBlank(message = "Action type is required")
    private String actionType;

    private String buildingName;
    private String floorName;
    private String details;
}