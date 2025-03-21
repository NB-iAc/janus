package dev.thesis.janus.central.dto;

import dev.thesis.janus.central.model.MapObjectType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ElevationMapObjectDTO {
    private Long id;
    private String name;
    private MapObjectType objectType;
    private Long nodeId;
}