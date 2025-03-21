package dev.thesis.janus.central.service;

import dev.thesis.janus.central.dto.UserDTO;
import dev.thesis.janus.central.exception.ResourceNotFoundException;
import dev.thesis.janus.central.model.Building;
import dev.thesis.janus.central.model.User;
import dev.thesis.janus.central.model.UserBuildingPermissions;
import dev.thesis.janus.central.repository.BuildingRepository;
import dev.thesis.janus.central.repository.UserBuildingPermissionsRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuildingAdminService {

    private static final Logger logger = LoggerFactory.getLogger(BuildingAdminService.class);

    private final BuildingRepository buildingRepository;
    private final UserBuildingPermissionsRepository userBuildingPermissionsRepository;

    

    @Transactional(readOnly = true)
    public List<UserDTO> getBuildingAdministrators(Long buildingId) {
        logger.debug("Retrieving administrators for building ID: {}", buildingId);

       

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + buildingId));

       

        List<UserBuildingPermissions> adminPermissions = userBuildingPermissionsRepository.findByBuildingIdWithAllAccess(buildingId);

       

        List<UserDTO> adminDTOs = adminPermissions.stream()
                .map(permission -> convertToUserDTO(permission.getUser()))
                .collect(Collectors.toList());

        logger.debug("Found {} administrators for building ID: {}", adminDTOs.size(), buildingId);

        return adminDTOs;
    }

    

    private UserDTO convertToUserDTO(User user) {
        return new UserDTO(
                user.getUserid(),
                user.getUsername(),
                user.getEmail(),
                user.getUsertoken(),
                null,

                user.getPictureUrl()
        );
    }
}