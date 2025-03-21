package dev.thesis.janus.central.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessRequestDTO {

    @NotNull
    private Long requestId;

    @NotNull
    private Boolean approved;
}