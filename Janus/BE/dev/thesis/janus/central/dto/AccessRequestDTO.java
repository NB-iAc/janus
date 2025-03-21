package dev.thesis.janus.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class AccessRequestDTO {
    private Long userId;
    private Long buildingId;
    private String roomId;
    private String accessType = "standard";

   

}