package dev.thesis.janus.central.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "building_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String actionType;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long buildingId;

    @Column
    private String buildingName;

    @Column
    private String floorName;

    @Column(columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    private LocalDateTime timestamp;
}