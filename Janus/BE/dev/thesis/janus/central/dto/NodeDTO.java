package dev.thesis.janus.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeDTO {
    private Long id;
    private Float x;
    private Float y;
    private Long floorId;
    private boolean isElevationNode;
    private String nodeType;
    private Set<Long> neighborIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}