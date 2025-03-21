package dev.thesis.janus.central.controller;

import dev.thesis.janus.central.dto.TenantDTO;
import dev.thesis.janus.central.service.UserPermissionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/protected/tenants")
@RequiredArgsConstructor
@Api(tags = "Tenant Management")
public class TenantController {

    private static final Logger logger = LoggerFactory.getLogger(TenantController.class);
    private final UserPermissionService userPermissionService;

    @GetMapping
    @ApiOperation("Get all tenants for buildings where the user is an admin")
    public ResponseEntity<List<TenantDTO>> getTenants(
            @RequestAttribute(name = "userId", required = true) Long userId) {

        logger.info("User {} requesting tenant list", userId);
        List<TenantDTO> tenants = userPermissionService.getTenantsByAdminId(userId);
        return ResponseEntity.ok(tenants);
    }

    @DeleteMapping("/{permissionId}")
    @ApiOperation("Revoke a tenant's access permission")
    public ResponseEntity<Map<String, Object>> revokeTenantPermission(
            @PathVariable Long permissionId,
            @RequestAttribute(name = "userId", required = true) Long userId) {

        logger.info("User {} attempting to revoke tenant permission {}", userId, permissionId);

        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = userPermissionService.revokeTenantPermission(userId, permissionId);

            response.put("status", "success");
            response.put("message", "Tenant permission successfully revoked");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error revoking tenant permission", e);

            response.put("status", "error");
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}