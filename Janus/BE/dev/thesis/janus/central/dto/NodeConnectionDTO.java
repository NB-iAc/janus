package dev.thesis.janus.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class NodeConnectionDTO {
    private Long sourceNodeId;
    private Long targetNodeId;
    private float distance;
    private boolean bidirectional = true;}