# EventZone (Monolithic)

Monorepo with a Spring Boot backend and React + TypeScript frontend.

## Run backend (dev - H2)
cd backend
mvn spring-boot:run

The backend uses H2 by default and seeds sample data from `data.sql`.

## Run frontend (dev)
cd frontend
npm install
npm run dev

## Notes
- Auth uses BCrypt for password hashing and a simple token issued by the backend (Authorization: Bearer <token>). This is a minimal, local-friendly approach.
