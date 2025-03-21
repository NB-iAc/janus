package dev.thesis.janus.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildingLogDTO {
    private Long id;
    private String actionType;
    private Long userId;
    private Long buildingId;
    private String buildingName;
    private String floorName;
    private String details;
    private LocalDateTime timestamp;
}