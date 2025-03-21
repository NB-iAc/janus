package dev.thesis.janus.central.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "map_objects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MapObjectType objectType;

    private String name;

    private String roomId;

    private String category = "DEFAULT";

    private String contactDetails;

    private String roomType;

    private String description;

    @ManyToOne
    @JoinColumn(name = "entrance_node_id")
    private Node entranceNode;

    @OneToMany(mappedBy = "mapObject", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MapObjectPoint> points = new ArrayList<>();

    private boolean accessible = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Override
    public String toString() {
        return "MapObject{" +
                "id=" + id +
                ", objectType=" + objectType +
                ", name='" + name + '\'' +
                ", roomId='" + roomId + '\'' +
                ", category='" + category + '\'' +
                '}';
    }
}