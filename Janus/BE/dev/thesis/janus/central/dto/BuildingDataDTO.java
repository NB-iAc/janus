

package dev.thesis.janus.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildingDataDTO {
    private BuildingDTO building;
    private List<FloorDTO> floors;
    private Map<Long, List<MapObjectDTO>> mapObjectsByFloor;
    private List<NodeDTO> nodes;
    private List<NodeConnectionDTO> connections;
}