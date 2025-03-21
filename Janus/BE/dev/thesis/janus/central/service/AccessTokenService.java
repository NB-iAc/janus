package dev.thesis.janus.central.service;

import dev.thesis.janus.central.model.AccessToken;
import dev.thesis.janus.central.model.Building;
import dev.thesis.janus.central.model.MapObject;
import dev.thesis.janus.central.model.User;
import dev.thesis.janus.central.repository.AccessTokenRepository;
import dev.thesis.janus.central.repository.BuildingRepository;
import dev.thesis.janus.central.repository.MapObjectRepository;
import dev.thesis.janus.central.repository.UserRepository;
import dev.thesis.janus.central.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AccessTokenService {

    private final AccessTokenRepository accessTokenRepository;
    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final MapObjectRepository mapObjectRepository;
    private final UserPermissionService userPermissionService;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

    @Transactional
    public AccessToken generateAccessToken(Long creatorId, Long buildingId, Long mapObjectId, Integer expirationDays) {
       

        if (buildingId == null && mapObjectId == null) {
            throw new IllegalArgumentException("Either buildingId or mapObjectId must be provided");
        }
        if (buildingId != null && mapObjectId != null) {
            throw new IllegalArgumentException("Only one of buildingId or mapObjectId should be provided");
        }

       

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Creator not found with id: " + creatorId));

       

        if (buildingId != null) {
            Building building = buildingRepository.findById(buildingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + buildingId));

           

            if (!userPermissionService.canUserAccessBuilding(creatorId, buildingId)) {
                throw new IllegalArgumentException("User does not have admin permissions for this building");
            }
        } else {
            MapObject mapObject = mapObjectRepository.findById(mapObjectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Map object not found with id: " + mapObjectId));

           

            if (!userPermissionService.canUserAccessMapObject(creatorId, mapObjectId)) {
                throw new IllegalArgumentException("User does not have admin permissions for this room");
            }
        }

       

        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        String token = base64Encoder.encodeToString(randomBytes);

       

        LocalDateTime expirationDate = null;
        if (expirationDays != null && expirationDays > 0) {
            expirationDate = LocalDateTime.now().plusDays(expirationDays);
        }

       

        AccessToken accessToken = new AccessToken();
        accessToken.setToken(token);
        accessToken.setCreator(creator);

        if (buildingId != null) {
            Building building = buildingRepository.findById(buildingId).get();
            accessToken.setBuilding(building);
        } else {
            MapObject mapObject = mapObjectRepository.findById(mapObjectId).get();
            accessToken.setMapObject(mapObject);
        }

        accessToken.setExpirationDate(expirationDate);

        return accessTokenRepository.save(accessToken);
    }
}