# Multi-Module Architecture

The dummy-off2on-redis project has been converted to a multi-module Maven architecture, following the patterns established in the off2on and off2on-api projects.

## Module Structure

```
dummy-off2on-redis (parent)
├── dummy-off2on-redis-model           # DTOs, Entities, Events
├── dummy-off2on-redis-service-api     # Service interfaces
├── dummy-off2on-redis-service-impl    # Service implementations
├── dummy-off2on-redis-config          # Spring configurations
├── dummy-off2on-redis-web             # REST controllers
└── dummy-off2on-redis-app             # Main application (executable)
```

## Module Details

### 1. dummy-off2on-redis-model
**Purpose**: Contains all data transfer objects, domain models, and event classes.

**Contents**:
- `dto/` - API request/response DTOs
- `model/` - Domain entities (GfdDeviceMapping, CartItem)
- `event/` - Event classes (CartEvent, EventType)
- `exception/` - Custom exception classes
- `repository/` - MongoDB repository interfaces

**Dependencies**:
- spring-boot-starter-data-mongodb
- jackson-databind
- lombok
- spring-webmvc (for SseEmitter)

### 2. dummy-off2on-redis-service-api
**Purpose**: Defines service interfaces for business logic.

**Contents**:
- `service/api/DeviceConnectionService.java` - Device connection service interface

**Dependencies**:
- dummy-off2on-redis-model
- spring-web

### 3. dummy-off2on-redis-service-impl
**Purpose**: Implements the service interfaces with business logic.

**Contents**:
- `service/impl/DeviceConnectionServiceImpl.java` - Device connection implementation
- `service/RedisSubscriberService.java` - Redis subscriber
- `service/OTPService.java` - OTP generation and validation
- `service/SSEService.java` - Server-Sent Events management

**Dependencies**:
- dummy-off2on-redis-service-api
- dummy-off2on-redis-model
- spring-boot-starter-data-redis
- spring-boot-starter-data-mongodb

### 4. dummy-off2on-redis-config
**Purpose**: Contains all Spring configuration classes.

**Contents**:
- `config/RedisConfig.java` - Redis configuration
- `config/SwaggerConfig.java` - OpenAPI/Swagger configuration
- `config/CorsConfig.java` - CORS configuration

**Dependencies**:
- dummy-off2on-redis-model
- dummy-off2on-redis-service-impl
- spring-boot-starter-data-redis
- spring-boot-starter-data-mongodb
- springdoc-openapi-starter-webmvc-ui

### 5. dummy-off2on-redis-web
**Purpose**: Contains REST controllers and web-related components.

**Contents**:
- `controller/HealthController.java` - Health check endpoint
- `controller/OTPController.java` - OTP generation/validation endpoints
- `controller/SSEController.java` - SSE connection endpoints
- `controller/GlobalExceptionHandler.java` - Global exception handler

**Dependencies**:
- dummy-off2on-redis-model
- dummy-off2on-redis-service-api
- dummy-off2on-redis-config
- spring-boot-starter-web
- springdoc-openapi-starter-webmvc-ui

### 6. dummy-off2on-redis-app
**Purpose**: Main Spring Boot application module that brings all modules together.

**Contents**:
- `DummyOff2onRedisApplication.java` - Main application class
- `application.properties` - Application configuration

**Dependencies**:
- dummy-off2on-redis-web
- dummy-off2on-redis-service-impl
- dummy-off2on-redis-config
- dummy-off2on-redis-model
- spring-boot-starter-web
- spring-boot-devtools

## Building the Project

### Build all modules
```bash
mvn clean install
```

### Build without tests
```bash
mvn clean install -DskipTests
```

### Build specific module
```bash
mvn clean install -pl dummy-off2on-redis-app
```

### Run the application
```bash
cd dummy-off2on-redis-app
mvn spring-boot:run
```

Or run the JAR:
```bash
java -jar dummy-off2on-redis-app/target/dummy-off2on-redis-app-0.0.1-SNAPSHOT.jar
```

## Benefits of Multi-Module Architecture

1. **Separation of Concerns**: Each module has a specific responsibility
2. **Reusability**: Modules can be reused in other projects
3. **Maintainability**: Easier to maintain and understand
4. **Build Optimization**: Can build/test modules independently
5. **Dependency Management**: Clear dependency hierarchy
6. **Team Collaboration**: Different teams can work on different modules
7. **Versioning**: Modules can be versioned independently

## Module Dependency Graph

```
dummy-off2on-redis-app
    ├── dummy-off2on-redis-web
    │   ├── dummy-off2on-redis-config
    │   │   ├── dummy-off2on-redis-service-impl
    │   │   │   ├── dummy-off2on-redis-service-api
    │   │   │   │   └── dummy-off2on-redis-model
    │   │   │   └── dummy-off2on-redis-model
    │   │   └── dummy-off2on-redis-model
    │   ├── dummy-off2on-redis-service-api
    │   │   └── dummy-off2on-redis-model
    │   └── dummy-off2on-redis-model
    ├── dummy-off2on-redis-service-impl
    │   ├── dummy-off2on-redis-service-api
    │   │   └── dummy-off2on-redis-model
    │   └── dummy-off2on-redis-model
    ├── dummy-off2on-redis-config
    │   ├── dummy-off2on-redis-service-impl
    │   │   ├── dummy-off2on-redis-service-api
    │   │   │   └── dummy-off2on-redis-model
    │   │   └── dummy-off2on-redis-model
    │   └── dummy-off2on-redis-model
    └── dummy-off2on-redis-model
```

## Migration Notes

- The original single-module structure has been backed up in `src-old-backup/`
- The original `pom.xml` has been backed up as `pom.xml.backup`
- All functionality remains the same, just organized into modules
- Application runs on the same port (8081) with the same endpoints
- Swagger UI is available at: http://localhost:8081/swagger-ui/index.html
- SpringDoc OpenAPI has been updated to version 2.8.9 for Spring Boot 3.5.6 compatibility

## Troubleshooting

### Build Failures
- Ensure you're using Java 21
- Run `mvn clean install` from the project root
- Check that all modules are listed in the parent pom.xml

### Runtime Issues
- Make sure all dependencies are correctly resolved
- Check application.properties for correct configuration
- Ensure Redis and MongoDB are running (if used)

## Future Enhancements

Consider adding these modules as the application grows:
- `dummy-off2on-redis-outbound-api` - External API interfaces
- `dummy-off2on-redis-outbound-impl` - External API implementations
- `dummy-off2on-redis-dao` - Data access layer
- `dummy-off2on-redis-common` - Common utilities

