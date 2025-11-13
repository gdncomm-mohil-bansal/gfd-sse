package com.gfd_sse.dummyoff2onredis.service.impl;

import com.gfd_sse.dummyoff2onredis.dto.ConnectionRequest;
import com.gfd_sse.dummyoff2onredis.dto.ConnectionResponse;
import com.gfd_sse.dummyoff2onredis.exception.DeviceConflictException;
import com.gfd_sse.dummyoff2onredis.exception.DeviceMappingNotFoundException;
import com.gfd_sse.dummyoff2onredis.exception.InvalidOtpException;
import com.gfd_sse.dummyoff2onredis.model.GfdDeviceMapping;
import com.gfd_sse.dummyoff2onredis.repository.GfdDeviceMappingRepository;
import com.gfd_sse.dummyoff2onredis.service.SSEService;
import com.gfd_sse.dummyoff2onredis.service.api.DeviceConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

/**
 * Implementation of DeviceConnectionService
 * Handles business logic for device connections and disconnections
 */
@Service
public class DeviceConnectionServiceImpl implements DeviceConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceConnectionServiceImpl.class);

    private final GfdDeviceMappingRepository gfdDeviceMappingRepository;
    private final SSEService sseService;

    public DeviceConnectionServiceImpl(
            GfdDeviceMappingRepository gfdDeviceMappingRepository,
            SSEService sseService) {
        this.gfdDeviceMappingRepository = gfdDeviceMappingRepository;
        this.sseService = sseService;
    }

    @Override
    @Transactional
    public ConnectionResponse connect(ConnectionRequest request) {
        logger.info("Processing connection request for destinationId: {} with OTP: {}", 
                request.getDestinationId(), request.getOtp() != null ? "provided" : "not provided");

        validateDestinationId(request.getDestinationId());

        if (StringUtils.hasText(request.getOtp())) {
            return connectWithOtp(request);
        }

        else {
            return reconnectWithoutOtp(request);
        }
    }

    /**
     * Handle first-time connection with OTP
     */
    private ConnectionResponse connectWithOtp(ConnectionRequest request) {
        logger.info("Processing first-time connection with OTP for destinationId: {}", 
                request.getDestinationId());

        Long otpValue = parseOtp(request.getOtp());

        Optional<GfdDeviceMapping> mappingOpt = gfdDeviceMappingRepository.findByOtp(otpValue);
        if (mappingOpt.isEmpty()) {
            logger.warn("SSE connection rejected: Invalid or expired OTP");
            throw new InvalidOtpException("Invalid or expired OTP. Please generate a new OTP from Front-liner app.");
        }

        GfdDeviceMapping mapping = mappingOpt.get();
        String sourceId = mapping.getSourceId();

        validateDeviceConflict(request.getDestinationId(), sourceId);

        // Update mapping with destination device ID and activate
        activateMapping(mapping, request.getDestinationId());

        logger.info("Device mapping activated. SourceId: {} connected to DestinationId: {}. OTP cleared to null.",
                sourceId, request.getDestinationId());

        // Create SSE emitter
        SseEmitter emitter = createOrReplaceEmitter(sourceId);

        return ConnectionResponse.builder()
                .sourceId(sourceId)
                .destinationId(request.getDestinationId())
                .emitter(emitter)
                .isReconnection(false)
                .build();
    }

    /**
     * Handle reconnection without OTP
     */
    private ConnectionResponse reconnectWithoutOtp(ConnectionRequest request) {
        logger.info("Processing reconnection without OTP for destinationId: {}", 
                request.getDestinationId());

        // Find existing mapping by destinationId
        Optional<GfdDeviceMapping> mappingOpt = gfdDeviceMappingRepository
                .findByDestinationId(request.getDestinationId());

        if (mappingOpt.isEmpty()) {
            logger.warn("SSE connection rejected: No device mapping found for destinationId: {}",
                    request.getDestinationId());
            throw new DeviceMappingNotFoundException(
                    "No active device mapping found. Please provide OTP for first-time connection.");
        }

        GfdDeviceMapping mapping = mappingOpt.get();

        // Validate mapping is active
//        if (!mapping.isActive()) {
//            logger.warn("SSE connection rejected: Device mapping is not active for destinationId: {}",
//                    request.getDestinationId());
//            throw new DeviceMappingNotFoundException(
//                    "Device mapping is not active. Please reconnect with OTP.");
//        }

      mapping.setActive(true);
      gfdDeviceMappingRepository.save(mapping);

        String sourceId = mapping.getSourceId();
        logger.info("Reconnecting using existing mapping. SourceId: {} -> DestinationId: {}",
                sourceId, request.getDestinationId());

        // Create SSE emitter
        SseEmitter emitter = createOrReplaceEmitter(sourceId);

        return ConnectionResponse.builder()
                .sourceId(sourceId)
                .destinationId(request.getDestinationId())
                .emitter(emitter)
                .isReconnection(true)
                .build();
    }

    /**
     * Validate destinationId is not null or empty
     */
    private void validateDestinationId(String destinationId) {
        if (!StringUtils.hasText(destinationId)) {
            logger.warn("SSE connection rejected: Missing destinationId");
            throw new IllegalArgumentException("Device ID (destinationId) is required");
        }
    }

    /**
     * Parse OTP string to Long
     */
    private Long parseOtp(String otp) {
        try {
            return Long.parseLong(otp);
        } catch (NumberFormatException e) {
            logger.warn("Invalid OTP format: {}", otp);
            throw new InvalidOtpException("Invalid OTP format");
        }
    }

    /**
     * Validate device conflict - check if destinationId already exists with different sourceId
     */
    private void validateDeviceConflict(String destinationId, String sourceId) {
        Optional<GfdDeviceMapping> existingMappingOpt = gfdDeviceMappingRepository
                .findByDestinationId(destinationId);

        if (existingMappingOpt.isPresent()) {
            GfdDeviceMapping existingMapping = existingMappingOpt.get();
            // If destinationId exists but belongs to different sourceId, reject connection
            if (!existingMapping.getSourceId().equals(sourceId)) {
                logger.warn("SSE connection rejected: DestinationId {} already connected to different sourceId: {}. " +
                        "Current OTP is for sourceId: {}", destinationId, existingMapping.getSourceId(), sourceId);
                throw new DeviceConflictException(
                        "This device is already connected to a different front-liner. " +
                        "Please disconnect first or use the correct OTP.");
            }
            // If same sourceId, it's a reconnection scenario
            logger.info("DestinationId {} reconnecting to same sourceId: {}", destinationId, sourceId);
        }
    }

    /**
     * Activate device mapping and clear OTP
     */
    private void activateMapping(GfdDeviceMapping mapping, String destinationId) {
        mapping.setDestinationId(destinationId);
        mapping.setActive(true);
        // Set OTP to null instead of 0 to prevent '000000' connections
        mapping.setOtp(null);
        gfdDeviceMappingRepository.save(mapping);
    }

    /**
     * Create or replace SSE emitter for sourceId
     */
    private SseEmitter createOrReplaceEmitter(String sourceId) {
        // Check if sourceId already has an active connection
        if (sseService.hasActiveConnection(sourceId)) {
            logger.info("SourceId {} already has an active connection. Closing old connection.", sourceId);
            sseService.removeEmitter(sourceId);
        }

        // Create new SSE emitter
        return sseService.createEmitter(sourceId);
    }

    @Override
    public boolean disconnect(String destinationId) {
        logger.info("Processing disconnect request for destinationId: {}", destinationId);

        if (!StringUtils.hasText(destinationId)) {
            logger.warn("Disconnect rejected: Missing destinationId");
            return false;
        }

        // Find device mapping to get sourceId
        Optional<GfdDeviceMapping> mappingOpt = gfdDeviceMappingRepository
                .findByDestinationId(destinationId);

        if (mappingOpt.isEmpty()) {
            logger.warn("No device mapping found for destinationId: {}", destinationId);
            return false;
        }

        String sourceId = mappingOpt.get().getSourceId();
        sseService.removeEmitter(sourceId);

        logger.info("SSE connection closed for sourceId: {} (destinationId: {})", sourceId, destinationId);
        return true;
    }

    @Override
    public boolean isConnected(String destinationId) {
        if (!StringUtils.hasText(destinationId)) {
            return false;
        }

        Optional<GfdDeviceMapping> mappingOpt = gfdDeviceMappingRepository
                .findByDestinationId(destinationId);

        if (mappingOpt.isEmpty()) {
            return false;
        }

        String sourceId = mappingOpt.get().getSourceId();
        return sseService.hasActiveConnection(sourceId);
    }
}

