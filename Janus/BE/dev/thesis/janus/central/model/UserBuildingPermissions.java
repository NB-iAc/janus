package dev.thesis.janus.central.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Table(name = "user_building_permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBuildingPermissions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userbuildingpermissionsid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userid", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buildingid", nullable = false)
    private Building building;

    @Column(name = "all_access", nullable = false)
    private boolean allAccess;
}