package dev.thesis.janus.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthRequest {
    private String email;     

    private String name;      

    private String picture;   

    private String google_id; 

}