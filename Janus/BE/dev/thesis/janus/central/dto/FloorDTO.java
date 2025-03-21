package dev.thesis.janus.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FloorDTO {
    private Long id;
    private Long buildingId;
    private Integer floorNumber;
    private String displayName;
    private boolean accessible;
}
