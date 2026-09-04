# EventZone — Backend API

EventZone is a Spring Boot 3 / Java 17 ticketing backend for managing events, ticket categories, bookings, and role-based access. The platform supports administrators, organisers, and attendees with protected business flows and consistent API responses.

Completed project updates

1. Standardized error handling
- Added a consistent error payload format across the API.
- All handled exceptions return a structured response with `timestamp`, `path`, `error`, and `message`.
- Validation errors now normalize to user-friendly messages such as `Field is required`.

Example response:
```json
{
  "timestamp": "2025-09-12T12:00:00Z",
  "path": "/api/events",
  "error": "VALIDATION_ERROR",
  "message": "Field is required"
}
```

2. Swagger/OpenAPI documentation
- Added response documentation for 200, 400, 403, 404, and 500 responses in controller Swagger annotations.
- Error payload schemas now appear in Swagger for validation, forbidden, not-found, and server error cases.
- The booking bulk-cancel endpoint is included in the generated OpenAPI docs.

3. Business validation improvements
- Prevent deleting categories that are still assigned to one or more events.
- Prevent deleting events when tickets are already booked against them.
- Show valid validation/conflict error messages instead of generic or random exceptions.
- Deletion conflicts now include contextual information such as assigned event names or booking references when available.

4. Booking cancellation enhancements
- Added `PUT /api/bookings/event/{eventId}/cancel` for organisers/admins to cancel all bookings for an event.
- This action restores available seats for the affected ticket categories after cancelling bookings.
- Single booking cancellation remains available through `PUT /api/bookings/{id}/cancel`.

5. Service test coverage
- Added test classes for services that were previously missing.
- Updated existing service tests to align with the repository and business-rule changes.
- Validation and guard conditions are covered for category deletion, event deletion, and booking cancellation flows.

Key features
- JWT-based authentication and authorization for ATTENDEE, ORGANISER, and ADMIN roles
- Event creation, update, and deletion management
- Ticket category and seat management
- Booking creation, viewing, and cancellation
- Secure admin and organiser operations
- H2 file-mode database for local development
- Actuator monitoring endpoints and a custom monitoring status endpoint

Tech stack
- Java 17+
- Spring Boot 3.3.x
- Spring Data JPA
- Spring Security + JWT
- H2 (local dev), PostgreSQL-ready configuration path
- Maven

Quick start

From the project root:

```bash
mvn spring-boot:run
```

Windows PowerShell:
```powershell
./scripts/run.ps1
```

Build jar:
```bash
mvn clean package
java -jar target/eventzone-backend.jar
```

Default behavior
- Runs on port 8080
- Uses the `dev` profile with an H2 file database at `data/eventzonedb`
- Seeds default users, categories, events, and sample data on start-up

Prerequisites
- Java 17 or newer
- Maven

Seeded accounts (password: `Password@123`)
- `admin@eventzone.com` — ADMIN
- `organiser1@eventzone.com` — ORGANISER
- `organiser2@eventzone.com` — ORGANISER
- `attendee1@eventzone.com` — ATTENDEE

Configuration highlights
- `server.port` default: `8080`
- `spring.datasource.url` (dev): `jdbc:h2:file:./data/eventzonedb;AUTO_SERVER=TRUE`
- JWT secret can be overridden via the `JWT_SECRET` environment variable

Important endpoints

Auth
- `POST /api/auth/register` — register an attendee
- `POST /api/auth/login` — returns `{ token, name, role, email }`
- `POST /api/auth/logout` — client-side token invalidation

Events
- `GET /api/events` — list events, optional `?category=` filter
- `GET /api/events/{id}` — fetch event details
- `POST /api/events` — create event (ORGANISER)
- `PUT /api/events/{id}` — update event (ORGANISER or ADMIN)
- `DELETE /api/events/{id}` — delete event (ORGANISER or ADMIN)

Ticket categories
- `GET /api/ticket-categories` — list categories
- `POST /api/ticket-categories` — create category
- `PUT /api/ticket-categories/{id}` — update category
- `DELETE /api/ticket-categories/{id}` — delete category (guarded by usage validation)

Bookings
- `POST /api/bookings` — create a booking
- `GET /api/bookings/mine` — get current user's bookings
- `PUT /api/bookings/{id}/cancel` — cancel a specific booking
- `PUT /api/bookings/event/{eventId}/cancel` — cancel all bookings for an event (ORGANISER or ADMIN)

Admin
- `GET /api/admin/categories` and related admin category management routes

Monitoring
- Human-friendly: `GET /api/monitoring/status`
- Actuator: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`

Swagger / API docs
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Testing
- Unit tests: `mvn test`
- Smoke tests: `./scripts/smoke-test.sh` or `./scripts/smoke-test.ps1`
- Postman collection: `postman_collection.json`

Project structure
- `src/main/java/com/eventzone` — application source code
- `src/test/java` — unit tests
- `scripts/` — setup and smoke-test scripts
- `data/` — local H2 database files
- `TESTING.md` — additional testing notes and usage details

Contributing
- Follow the existing project conventions.
- Add or update tests for new behavior and validation logic.

License
- See the repository license file if present.

## Test coverage
- Overall instruction coverage (JaCoCo): 79.46% (1153 covered / 1451 total)

## Api response examples
- "endpoints": [
  {
  "method": "OPTIONS",
  "path": "/api/events/{id}",
  "status": "200",
  "outcome": "SUCCESS",
  "totalResponseTimeMs": 754.79
  },
  {
  "method": "OPTIONS",
  "path": "/api/organiser/events",
  "status": "200",
  "outcome": "SUCCESS",
  "totalResponseTimeMs": 656.69
  },
  {
  "method": "GET",
  "path": "/api/monitoring/status",
  "status": "200",
  "outcome": "SUCCESS",
  "totalResponseTimeMs": 1237.22
  },
  {
  "method": "PUT",
  "path": "/api/bookings/event/{eventId}/cancel",
  "status": "200",
  "outcome": "SUCCESS",
  "totalResponseTimeMs": 674.28
  }
  ]


