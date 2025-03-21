package dev.thesis.janus.central.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Table(name = "user_room_permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoomPermissions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userroompermissionsid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userid", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "map_object_id", nullable = false)
    private MapObject mapObject; 

}