package dev.thesis.janus.central.controller;

import com.fasterxml.jackson.databind.JsonNode;
import dev.thesis.janus.central.dto.BuildingDataDTO;
import dev.thesis.janus.central.service.TransformerDataService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transformer-data")
@RequiredArgsConstructor
@Api(tags = "Transformer Data Management")
public class TransformerDataController {
    private static final Logger logger = LoggerFactory.getLogger(TransformerDataController.class);

    private final TransformerDataService transformerDataService;

    @PostMapping
    @ApiOperation("Upload building data in transformer format")
    public ResponseEntity<BuildingDataDTO> uploadTransformerData(@RequestBody JsonNode transformerData) {
        logger.info("Received transformer format data upload request");
        try {
            BuildingDataDTO buildingData = transformerDataService.processTransformerData(transformerData);
            logger.info("Successfully processed transformer data for building ID: {}",
                    buildingData.getBuilding().getId());
            return new ResponseEntity<>(buildingData, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error processing transformer data upload", e);
            throw e;
        }
    }
}