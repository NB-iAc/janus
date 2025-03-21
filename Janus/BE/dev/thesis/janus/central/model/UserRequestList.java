package dev.thesis.janus.central.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Table(name = "user_request_list")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requesting_user_id", nullable = false)
    private User requestingUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(name = "roomid", nullable = false)
    private String roomId;

    @Column(name = "accesstype", nullable = false, length = 50)
    private String accessType;

    @Column(name = "status", nullable = false, length = 50)
    private String status;
}