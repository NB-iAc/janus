package dev.thesis.janus.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;           

    private String name;
    private String email;
    private String usertoken;  

    private String userdetails;

    private String pictureUrl; 

}