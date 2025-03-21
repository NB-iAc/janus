package dev.thesis.janus.central.service;

import dev.thesis.janus.central.dto.CreateBuildingLogDTO;
import dev.thesis.janus.central.dto.BuildingLogDTO;
import dev.thesis.janus.central.model.BuildingLog;
import dev.thesis.janus.central.model.User;
import dev.thesis.janus.central.repository.BuildingLogRepository;
import dev.thesis.janus.central.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuildingLogService {

    private final BuildingLogRepository buildingLogRepository;
    private final UserRepository userRepository;

    private String getUsernameById(Long userId) {
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElse("Unknown User");
    }
    @Transactional
    public BuildingLogDTO createBuildingLog(CreateBuildingLogDTO logDTO) {
        BuildingLog log = new BuildingLog();
        log.setUserId(logDTO.getUserId());
        log.setBuildingId(logDTO.getBuildingId());
        log.setActionType(logDTO.getActionType());
        log.setBuildingName(logDTO.getBuildingName());
        log.setFloorName(logDTO.getFloorName());
        log.setDetails(logDTO.getDetails());

        BuildingLog savedLog = buildingLogRepository.save(log);
        return convertToDTO(savedLog);
    }

    @Transactional(readOnly = true)
    public List<BuildingLogDTO> getBuildingLogs(Long buildingId) {
        return buildingLogRepository.findByBuildingIdOrderByTimestampDesc(buildingId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private BuildingLogDTO convertToDTO(BuildingLog log) {
        return new BuildingLogDTO(
                log.getId(),
                log.getActionType(),
                log.getUserId(),
                log.getBuildingId(),
                log.getBuildingName(),
                log.getFloorName(),
                log.getDetails(),
                log.getTimestamp()
        );
    }
}