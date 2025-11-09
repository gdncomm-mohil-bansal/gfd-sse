package com.gfd_sse.dummyoff2onredis.repository;

import com.gfd_sse.dummyoff2onredis.model.GfdDeviceMapping;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for GFD Device Mapping
 * Provides access to device-to-device mappings stored in MongoDB
 */
@Repository
public interface GfdDeviceMappingRepository extends MongoRepository<GfdDeviceMapping, String> {

    /**
     * Find device mapping by OTP
     *
     * @param otp the OTP generated for device connection
     * @return Optional containing the device mapping if found
     */
    Optional<GfdDeviceMapping> findByOtp(long otp);

    /**
     * Find device mapping by source device ID
     *
     * @param sourceId the source device ID (Front-liner's device)
     * @return Optional containing the device mapping if found
     */
    Optional<GfdDeviceMapping> findBySourceId(String sourceId);

    /**
     * Find active device mapping by source device ID
     *
     * @param sourceId the source device ID
     * @param isActive the active status
     * @return Optional containing the active device mapping if found
     */
    Optional<GfdDeviceMapping> findBySourceIdAndIsActive(String sourceId, boolean isActive);

    /**
     * Find device mapping by destination device ID (GFD's device)
     *
     * @param destinationId the destination device ID
     * @return Optional containing the device mapping if found
     */
    Optional<GfdDeviceMapping> findByDestinationId(String destinationId);
}
