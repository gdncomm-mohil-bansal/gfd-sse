package com.gfd_sse.dummyoff2onredis.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Model for GFD Device Mapping
 * Maps Front-liner device (source) to GFD PWA device (destination)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = GfdDeviceMapping.COLLECTION_NAME)
public class GfdDeviceMapping {

    public static final String COLLECTION_NAME = "gfd_device_mapping";

    @Id
    private String id;

    /**
     * Source device ID (Front-liner's device) - unique index
     */
    @Indexed(unique = true, sparse = true)
    private String sourceId;

    /**
     * Destination device ID (GFD PWA device) - unique index
     */
    @Indexed(unique = true, sparse = true)
    private String destinationId;

    /**
     * Connection active status
     */
    private boolean isActive;

    /**
     * OTP for device connection (nullable to prevent '000000' connections)
     */
    private Long otp;
}
