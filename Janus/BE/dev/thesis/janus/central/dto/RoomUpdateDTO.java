package dev.thesis.janus.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;



@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomUpdateDTO {
    @NotBlank(message = "Room name is required")
    private String name;

    private String description;

    private String contactDetails;

    private String roomType;
}