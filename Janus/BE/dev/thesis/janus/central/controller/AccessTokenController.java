package dev.thesis.janus.central.controller;

import dev.thesis.janus.central.model.AccessToken;
import dev.thesis.janus.central.service.AccessTokenService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/access-tokens")
@RequiredArgsConstructor
@Api(tags = "Access Token Management")
public class AccessTokenController {

    private final AccessTokenService accessTokenService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @PostMapping("/generate")
    @ApiOperation("Generate a new access token")
    public ResponseEntity<Map<String, Object>> generateAccessToken(
            @RequestParam Long creatorId,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long mapObjectId,
            @RequestParam(required = false, defaultValue = "30") Integer expirationDays) {

        try {
            AccessToken token = accessTokenService.generateAccessToken(
                    creatorId, buildingId, mapObjectId, expirationDays);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Access code generated successfully");
            response.put("accessCode", token.getToken());

            if (token.getExpirationDate() != null) {
                response.put("expiresAt", token.getExpirationDate().format(DATE_FORMATTER));
            } else {
                response.put("expiresAt", null);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
}