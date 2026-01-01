# DAP Prototype Spring Boot sample

![Coverage](docs/coverage-badge.svg)

A minimal Spring Boot app that matches the OpenAPI `POST /request` endpoint.

## Run

```bash
./mvnw spring-boot:run
# or if you have Maven installed
mvn spring-boot:run
```

The app runs on `http://localhost:8080/api`.

## Sample request

```bash
curl -X POST "http://localhost:8080/api/request" \
  -H "Content-Type: application/json" \
  -d '{"requestInfo": {"activityId": "abcd", "activityTimeStamp": "2025-12-30 13:36:00"}}'
```

Expected response:

```json
{
  "success": true,
  "message": "Request processed successfully"
}
```
