package dev.thesis.janus.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List; 

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestListDTO {
    private List<UserRequestItemDTO> items;
}
