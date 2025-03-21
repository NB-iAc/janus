package dev.thesis.janus.central.repository;

import dev.thesis.janus.central.model.BuildingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuildingLogRepository extends JpaRepository<BuildingLog, Long> {

    List<BuildingLog> findByBuildingIdOrderByTimestampDesc(Long buildingId);
}