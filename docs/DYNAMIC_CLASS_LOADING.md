# Dynamic Class Loading with TxnClassLoader

## Overview

This application uses a custom `URLClassLoader` called `TxnClassLoader` to dynamically load transaction model classes from the `dap-prototype-txn-models` module. This approach provides isolation and flexibility for loading different versions of transaction models at runtime.

## Architecture

### Class Loader Hierarchy

```
Bootstrap ClassLoader
    ↑
System/Application ClassLoader (loads app classes)
    ↑
TxnClassLoader (loads txn-models classes dynamically)
```

The `TxnClassLoader` extends `URLClassLoader` and uses the system class loader as its parent, following the standard parent delegation model.

### Key Components

#### 1. TxnClassLoader
- **Location**: `com.example.dapprototype.classloader.TxnClassLoader`
- **Purpose**: Custom URLClassLoader for loading transaction model classes
- **Parent**: System ClassLoader
- **Loads**: Classes from `dap-prototype-txn-models` module

#### 2. TxnClassLoaderService
- **Location**: `com.example.dapprototype.classloader.TxnClassLoaderService`
- **Purpose**: Manages the lifecycle of TxnClassLoader
- **Features**:
  - Auto-detects txn-models module location
  - Configurable via `txn.classloader.paths` property
  - Provides methods to load classes and create instances
  - Properly closes class loader on application shutdown

#### 3. RequestProcessingService (Refactored)
- **Location**: `com.example.dapprototype.service.RequestProcessingService`
- **Changes**:
  - Removed static imports of `RequestPayload` and `RequestMapper`
  - Dynamically loads these classes using `TxnClassLoader`
  - Uses reflection to deserialize JSON and invoke mapper methods
  - Logs class loader information for debugging

## How It Works

### Initialization Flow

1. **Application Startup**
   - `TxnClassLoaderService` is initialized as a Spring bean
   - `@PostConstruct` method creates `TxnClassLoader` with appropriate URLs
   - Default path: `../dap-prototype-txn-models/target/classes`

2. **Service Initialization**
   - `RequestProcessingService` constructor calls `initializeDynamicClasses()`
   - Loads `RequestPayload.class` dynamically
   - Loads `RequestMapper.class` dynamically
   - Retrieves `RequestMapper.INSTANCE` (MapStruct generated)

3. **Request Processing**
   - JSON deserialization uses the dynamically loaded `RequestPayload` class
   - Mapping uses reflection to invoke `requestMapper.toCustomerRequest(payload)`
   - Result is cast to `CustomerRequest` (loaded by app class loader)

### Dynamic Loading Code Example

```java
// Load the class
Class<?> requestPayloadClass = txnClassLoaderService.loadClass(
    "com.example.dapprototype.model.RequestPayload"
);

// Deserialize JSON to the dynamically loaded class
Object payload = objectMapper.readValue(rawBody, requestPayloadClass);

// Use reflection to invoke mapper method
Method mapperMethod = requestMapperInstance.getClass()
    .getMethod("toCustomerRequest", requestPayloadClass);
Object result = mapperMethod.invoke(requestMapperInstance, payload);
```

## Configuration

### application.properties

```properties
# Optional: Specify custom paths for txn-models classes
# Comma-separated list of paths
txn.classloader.paths=/path/to/custom/classes,/another/path

# If not specified, auto-detection will find:
# - ../dap-prototype-txn-models/target/classes
# - ../../dap-prototype-txn-models/target/classes
# - /workspaces/dapprototypev2/dap-prototype-txn-models/target/classes
```

## Benefits

1. **Isolation**: Transaction model classes are loaded in a separate class loader
2. **Flexibility**: Can load different versions of txn-models at runtime
3. **Hot Reload**: Potentially reload classes without restarting the application
4. **Clean Separation**: App logic is decoupled from txn-models implementation

## Logging

The implementation includes comprehensive logging to track class loading:

```
INFO  c.e.d.c.TxnClassLoaderService - TxnClassLoader initialized successfully with 1 URLs
DEBUG c.e.d.c.TxnClassLoaderService - TxnClassLoader URL: file:/path/to/txn-models/target/classes/
INFO  c.e.d.s.RequestProcessingService - Loaded com.example.dapprototype.model.RequestPayload using com.example.dapprototype.classloader.TxnClassLoader
INFO  c.e.d.s.RequestProcessingService - Loaded com.example.dapprototype.mapper.RequestMapper using com.example.dapprototype.classloader.TxnClassLoader
```

## Building and Running

### Build the txn-models module first:
```bash
cd dap-prototype-txn-models
mvn clean install
```

### Build and run the application:
```bash
cd dap-prototype-app
mvn clean install
mvn spring-boot:run
```

The application will:
1. Initialize `TxnClassLoader` with the txn-models classes
2. Dynamically load `RequestPayload` and `RequestMapper`
3. Process requests using reflection

## Testing

The dynamic class loading is transparent to the existing tests. All tests should pass without modification because:
- The functionality remains the same
- Only the loading mechanism has changed
- The API and behavior are preserved

Run tests:
```bash
mvn test
```

## Troubleshooting

### ClassNotFoundException

If you see `ClassNotFoundException` for txn-models classes:
1. Ensure txn-models is built: `cd dap-prototype-txn-models && mvn install`
2. Check the logs for TxnClassLoader initialization
3. Verify the path: `/workspaces/dapprototypev2/dap-prototype-txn-models/target/classes`
4. Set explicit path in application.properties if needed

### NoSuchMethodException

If reflection fails:
1. Ensure txn-models is compiled with the same Java version
2. Check that MapStruct has generated the mapper implementation
3. Verify the method signature matches exactly

### ClassLoader Issues

Enable debug logging:
```properties
logging.level.com.example.dapprototype.classloader=DEBUG
logging.level.com.example.dapprototype.service=DEBUG
```

## Future Enhancements

1. **Hot Reload**: Implement class reloading without application restart
2. **Version Management**: Load multiple versions of txn-models simultaneously
3. **JAR Loading**: Load txn-models from JAR files instead of directories
4. **Dynamic Configuration**: Reload configuration when txn-models change
