package dev.thesis.janus.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor; 

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestItemDTO {
    private Long requestId;
    private Long buildingId;
    private String buildingName;
    private String accessType;
    private String roomId;
    private String roomName;
    private String status;
}
