# EventZone (Monolithic)

Build an event ticket booking web application where users can browse upcoming events
(concerts, sports, workshops), choose a ticket category, and register/book tickets.
Event organisers manage their event listings. Admins manage categories and event visibility.

Monorepo with a Spring Boot backend and React + TypeScript frontend.

## Run everything on one port (http://localhost:8080)

The backend serves the built React app from the same origin as the API, so there is no
dev proxy and no CORS involved.

```
cd frontend
npm install
npm run build          # produces frontend/dist

cd ../backend
mvn spring-boot:run    # http://localhost:8080  -> UI + API
```

Or build a single self-contained jar:

```
cd frontend && npm run build
cd ../backend && mvn clean package
java -jar target/backend-1.0.0.jar
```

The Maven build copies `frontend/dist` into `classpath:/static` (see the
`copy-frontend-build` execution in `backend/pom.xml`). Re-run `npm run build` after
changing frontend code, then restart the backend to pick it up.

If `frontend/dist` is missing, Maven logs a warning, skips the copy, and the backend
still starts as an API-only service.

## Run with hot reload (two ports)

For frontend development you usually want Vite's hot reload instead of rebuilding.
Vite proxies `/api` to the backend, so the app still behaves as one origin:

```
cd backend && mvn spring-boot:run     # http://localhost:8080  (API)
cd frontend && npm run dev            # http://localhost:5173  (UI, proxies /api -> 8080)
```

Both URLs work; 8080 serves the last `npm run build`, 5173 serves live code.

## Seeded accounts

All three share the password `Password123!`:

| Email | Role | Can do |
| --- | --- | --- |
| `admin@eventzone.com` | ADMIN | Everything, plus category CRUD, event activation, user roles |
| `org1@eventzone.com` | ORGANISER | Create/edit/delete own events and their ticket categories |
| `user1@eventzone.com` | ATTENDEE | Browse, book, cancel own bookings |

New accounts can self-register as **ATTENDEE** or **ORGANISER** via
`POST /api/auth/register` (`role` is optional and defaults to `ATTENDEE`).
`ADMIN` cannot be self-assigned; an existing admin grants it via
`PUT /api/users/{id}/role`.

A role change takes effect on the user's **next login**, since the role travels
inside the JWT.

## Database

H2, file-backed at `backend/data/eventzone.mv.db`, so accounts and bookings survive
restarts. `data.sql` re-runs on every startup and is written to insert only missing
rows, so it never overwrites live data.

**Delete `backend/data/` to reset to a clean seeded database.** Do this after pulling
schema changes.

Console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/eventzone`,
user `sa`, no password).

## Configuration

| Setting | Default | Override |
| --- | --- | --- |
| Server port | 8080 | `PORT` env var, or `-Dspring-boot.run.arguments=--server.port=8081` |
| JWT secret | dev placeholder | `JWT_SECRET` env var — **must** be set outside local dev |
| JWT lifetime | 24h | `JWT_EXPIRATION_MS` |
| Frontend API base | same origin | `VITE_API_BASE` (only needed if the UI is hosted apart from the API) |

## API

Auth
- `POST /api/auth/register` — `{email, password, name, role?}` -> 201
- `POST /api/auth/login` -> 200 `{token, name, role, email}`
- `POST /api/auth/logout` -> 204

Events
- `GET /api/events` — public; optional `?category=`; hides deactivated events
- `GET /api/events/{id}` — public, includes ticket categories
- `GET /api/events/mine` — organiser's own events
- `GET /api/events/all` — admin; includes deactivated
- `POST /api/events` / `PUT /api/events/{id}` / `DELETE /api/events/{id}` — owner or admin
- `PUT /api/events/{id}/active` — admin; activate/deactivate

Ticket categories
- `GET /api/events/{id}/ticket-categories` — public
- `POST /api/events/{id}/ticket-categories` — owner or admin
- `PUT`/`DELETE /api/ticket-categories/{id}` — owner or admin

Categories
- `GET /api/categories` — public
- `POST /api/categories`, `PUT`/`DELETE /api/categories/{id}` — admin

Bookings
- `POST /api/bookings` — `{ticketCategoryId, quantity}` (1-5)
- `GET /api/bookings/mine`
- `PUT /api/bookings/{id}/cancel` — restores seats

Users
- `GET /api/users`, `PUT /api/users/{id}/role` — admin

Every failure returns the same shape:

```json
{ "timestamp": "2025-09-12T12:00:00Z", "path": "/api/...", "error": "VALIDATION_ERROR", "message": "Field is required" }
```

## Tests

```
cd backend && mvn test      # 49 tests
cd frontend && npx tsc --noEmit
```

## Notes

- Passwords are BCrypt hashed; auth is a stateless JWT sent as `Authorization: Bearer <token>`.
- Organisers may only manage events they own; admins may manage any.
- Deleting an event or ticket category with bookings against it is refused (409) rather
  than orphaning those bookings.
- Changing a ticket category's capacity preserves seats already sold, and is refused if
  the new total is below that number.
- The JWT is stored in `localStorage`, which is readable by any script on the page.
  Acceptable for local development; a production deployment should move to an
  httpOnly cookie.
