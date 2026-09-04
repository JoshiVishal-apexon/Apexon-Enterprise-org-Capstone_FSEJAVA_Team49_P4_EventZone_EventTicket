# EventZone Backend — Testing Guide

Three complementary layers of testing are included. Run them in this order for the
fastest feedback loop.

> **Windows PowerShell users**: `mvn` and `./scripts/*.sh` won't work directly in
> PowerShell (no bash, and if Maven itself isn't installed, `mvn` won't be
> recognized either). Use the `.ps1` equivalents instead — `.\scripts\test.ps1` and
> `.\scripts\smoke-test.ps1` — which auto-download a local copy of Maven the first
> time if it's not already on your PATH. See README.md §2a for execution-policy
> notes if PowerShell refuses to run the script at all.

## 1. Unit tests (JUnit 5 + Mockito) — fastest, no server needed

```bash
mvn test              # macOS/Linux/Git Bash/WSL
.\scripts\test.ps1    # Windows PowerShell
```

These mock the repository layer and test business logic in isolation:

| Test class | What it covers |
|---|---|
| `AuthServiceTest` | Registering a duplicate email throws `ConflictException`; successful login returns a token; wrong password throws `BadCredentialsException`. |
| `EventServiceTest` | Creating an event associates the calling organiser; updating/deleting an event you don't own (and aren't ADMIN) throws `ForbiddenException`. |
| `BookingServiceTest` | Booking decrements `availableSeats` and generates a unique `bookingRef`; booking more than `availableSeats` or outside the 1-5 quantity range throws `BadRequestException`; cancelling restores seats; cancelling an already-cancelled booking throws `BadRequestException`. |

Run a single class: `mvn test -Dtest=BookingServiceTest`

## 2. End-to-end smoke test — proves the whole app actually works

```bash
./scripts/smoke-test.sh     # macOS/Linux/Git Bash/WSL (bash + curl)
.\scripts\smoke-test.ps1    # Windows PowerShell (native, no bash needed)
```

Both scripts do exactly the same thing — one's bash+curl, the other's PowerShell —
pick whichever matches your shell.

Unlike the unit tests (which mock the database), this builds the real jar, starts it
against the real embedded H2 database, and drives the actual HTTP API end-to-end
through the full happy path: login as each seeded role, register + log in a new
attendee, browse categories/events, an organiser creates an event and a ticket
category, an attendee books tickets, tries to over-book (expects 400), views and
cancels their booking, tries to cancel it twice (expects 400), confirms an
unauthenticated request is rejected (401) and a wrong-role request is rejected (403),
checks the organiser dashboard, and exercises the admin activate/deactivate +ticket
category/event delete endpoints — then shuts the app down. It prints a `[PASS]`/`[FAIL]`
line per step and a final count, so a single command tells you definitively whether
the API works, not just whether it compiles.

Requires (bash version): Java 17+, Maven, curl, python3 (only used to pull a field
like `id` or `token` out of a JSON response). Requires (PowerShell version): Java 17+
only — Maven is auto-downloaded if missing, and JSON parsing uses PowerShell's
built-in `ConvertFrom-Json`.

## 3. Manual / exploratory testing (Postman)

Import `postman_collection.json` into Postman (or run it with
[Newman](https://www.npmjs.com/package/newman): `newman run postman_collection.json`).

- Every request has a `{{baseUrl}}` variable (defaults to `http://localhost:8080`).
- The three "Login as ..." requests under **Auth** each have a Postman **Test**
  script that automatically saves the returned JWT into `{{token}}` plus a
  role-specific variable (`{{adminToken}}`, `{{organiserToken}}`, `{{attendeeToken}}`)
  — no manual copy/paste needed.
- Several other requests (list categories/events, get event detail, create
  event/ticket-category/booking/category) also auto-save the id they return into
  `{{categoryId}}` / `{{eventId}}` / `{{ticketCategoryId}}` / `{{bookingId}}`, so you
  can click through a folder top-to-bottom without manually filling in IDs.
- **Caveat**: the `Delete ...` requests are real deletes. If you use Collection
  Runner to fire the *entire* collection back-to-back, a Delete request in one
  folder will remove a resource that a later folder still references by id. For a
  non-destructive walkthrough, either deselect the Delete requests in Runner, or
  click through requests individually in the order listed (Auth → Categories →
  Events → Ticket Categories → Bookings → Organiser Dashboard → Admin), leaving
  Delete requests for last.

## Coverage summary

| Layer | Tool | What it proves |
|---|---|---|
| Unit | JUnit5 + Mockito | Business logic is correct in isolation (seat math, ownership checks, auth rules) |
| Integration/E2E | `scripts/smoke-test.sh` or `.ps1` | The real app, wired together with a real database, handles the full user journey and returns the right HTTP status at every step |
| Manual/exploratory | Postman collection | Every endpoint is reachable and documented; convenient for ad-hoc poking during development |
