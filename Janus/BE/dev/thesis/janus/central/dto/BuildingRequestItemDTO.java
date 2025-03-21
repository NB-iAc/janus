package dev.thesis.janus.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor; 

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildingRequestItemDTO {
    private Long requestId;
    private RequestUserDTO user;
    private Long buildingId;
    private String buildingName;
    private String accessType;
    private String roomId;
    private String status;
}
