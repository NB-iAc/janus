package dev.thesis.janus.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildingWithRoomsDTO {
    private Long buildingId;
    private String buildingName;
    private String buildingDescription;
    private List<RoomDTO> rooms;
}