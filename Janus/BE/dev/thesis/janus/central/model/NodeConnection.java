package dev.thesis.janus.central.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
@Entity
@Table(name = "node_connections")
@Data 

@NoArgsConstructor
@AllArgsConstructor
public class NodeConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "source_node_id", nullable = false)
    private Node sourceNode;

    @ManyToOne
    @JoinColumn(name = "target_node_id", nullable = false)
    private Node targetNode;

   

    @Column(nullable = false, insertable = true, updatable = true)
    private Float distance;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean bidirectional = true;
}