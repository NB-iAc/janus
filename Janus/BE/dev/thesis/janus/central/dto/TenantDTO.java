package dev.thesis.janus.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantDTO {
    private Long userId;
    private String userName;
    private String userEmail;
    private Long buildingId;
    private String buildingName;
    private Integer floorNumber;
    private String floorName;
    private Long roomId;
    private String roomName;
    private Long userRoomPermissionsId;
}