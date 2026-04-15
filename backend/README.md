# Earthquake Dashboard Backend

Spring Boot backend that fetches recent earthquake data from the USGS GeoJSON feed, filters it, stores it in PostgreSQL, and exposes REST endpoints for refresh/list/delete operations.

## Tech Stack

- Java 21
- Spring Boot 3.5
- Spring Data JPA
- PostgreSQL
- Gradle (wrapper included)
- JUnit 5 + Spring test support

## Features Implemented

- Fetches data from USGS:
  - `https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_hour.geojson`
- Parses and stores key fields:
  - magnitude, magType, place, title, eventTime
  - plus latitude, longitude, depth, fetchedAt
- Filters earthquakes by:
  - magnitude strictly greater than `2.0` (configurable)
  - event time after configured `since` threshold
- Replaces stored data on refresh:
  - deletes existing rows, then inserts freshly filtered rows
- Optional delete by USGS id
- Centralized JSON error responses via global exception handler

## Project Configuration

Main config file: `src/main/resources/application.properties`

Local secrets/config should go in:
- `application-local.properties` (gitignored)

Sample file provided:
- `application-local.properties.example`

### Local setup (recommended)

```bash
cp application-local.properties.example application-local.properties
```

Then edit `application-local.properties` with your real local DB credentials.

Optional USGS timeout overrides:

```ini
earthquake.usgs.connect-timeout-ms=3000
earthquake.usgs.request-timeout-ms=5000
```

## PostgreSQL Setup

Create DB/user (example):

```bash
sudo -u postgres psql -p 5433
```

```sql
CREATE USER earthquake_app WITH PASSWORD 'your_password';
CREATE DATABASE earthquake_dashboard OWNER earthquake_app;
GRANT ALL PRIVILEGES ON DATABASE earthquake_dashboard TO earthquake_app;
\q
```

Example `application-local.properties`:

```ini
spring.datasource.url=jdbc:postgresql://127.0.0.1:5433/earthquake_dashboard
spring.datasource.username=earthquake_app
spring.datasource.password=your_password
```

## Run Backend

```bash
./gradlew bootRun
```

App starts on:
- `http://localhost:8080`

## API Endpoints

Base path: `/api/earthquakes`

- `GET /api/earthquakes`
  - Returns stored earthquakes (latest first)
  - Optional query params:
    - `minMagnitude` (number, must be `>= 0`, strict filter uses `>`)
    - `startTime` (ISO-8601 timestamp, e.g. `2026-04-15T10:00:00Z`)
    - `endTime` (ISO-8601 timestamp)
  - Invalid query formats (for example non-ISO `startTime`) return `400 Bad Request`
  - Invalid filter combinations (for example `startTime > endTime`) return `400 Bad Request`
  - `200 OK`

- `POST /api/earthquakes/refresh`
  - Fetches from USGS, filters, replaces DB records, returns refreshed list
  - `200 OK`

- `DELETE /api/earthquakes/{usgsId}`
  - Deletes specific record by USGS id
  - `204 No Content` if deleted
  - `404 Not Found` if missing

## Error Response Format

Errors are returned as JSON:

```json
{
  "timestamp": "2026-04-15T10:00:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to call USGS API.",
  "path": "/api/earthquakes/refresh"
}
```

## Tests

Run all tests:

```bash
./gradlew test
```

Run service integration test only:

```bash
./gradlew test --tests com.ermal.backend.service.EarthquakeServiceIntegrationTest
```

Run controller test only:

```bash
./gradlew test --tests com.ermal.backend.controller.EarthquakeControllerTest
```

## Assumptions

- PostgreSQL is available locally.
- Local credentials are provided via `application-local.properties` or environment variables.
- USGS API availability is external and dynamic.

