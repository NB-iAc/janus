package dev.thesis.janus.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomDTO {
    private Long id;
    private String roomId = "";
    private String name = "";
    private String description = "";
    private Long floorId;
    private Integer floorNumber;
    private String floorName = "";
    private String category = "";
    private String roomType = "";
    private String contactDetails = "";
    private boolean accessible;
    private Long entranceNodeId;

   

    public void setEntranceNodeId(Long entranceNodeId) {
        this.entranceNodeId = entranceNodeId;
    }
}