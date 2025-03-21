package dev.thesis.janus.central.repository;

import dev.thesis.janus.central.model.MapObject;
import dev.thesis.janus.central.model.MapObjectPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MapObjectPointRepository extends JpaRepository<MapObjectPoint, Long> {
    List<MapObjectPoint> findByMapObjectOrderByPointOrder(MapObject mapObject);

    List<MapObjectPoint> findByMapObjectIdOrderByPointOrder(Long mapObjectId);

    void deleteByMapObjectId(Long mapObjectId);
   


    

    @Query("SELECT p FROM MapObjectPoint p WHERE p.mapObject.id IN :mapObjectIds ORDER BY p.pointOrder")
    List<MapObjectPoint> findAllByMapObjectIdIn(List<Long> mapObjectIds);








}
