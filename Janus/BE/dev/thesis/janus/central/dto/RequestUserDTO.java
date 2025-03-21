package dev.thesis.janus.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;



@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestUserDTO {
    private Long id;
    private String name;
    private String email;
}

