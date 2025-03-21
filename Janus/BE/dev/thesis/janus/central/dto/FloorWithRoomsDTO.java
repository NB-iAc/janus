package dev.thesis.janus.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FloorWithRoomsDTO {
    private Long floorId;
    private Integer floorNumber;
    private String floorName;
    private List<SimpleRoomDTO> rooms;
}
