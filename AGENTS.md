# Repository Guidelines

## Project Structure & Module Organization
- Backend: Spring Boot under `src/main/java` and resources in `src/main/resources`.
- Backend tests: `src/test/java` with fixtures in `src/test/resources`.
- Frontend (Next.js + TypeScript): `frontend/` (pages in `frontend/app`, UI in `frontend/components`, tests in `frontend/tests`).
- Ops: Docker files (`Dockerfile.backend`, `frontend/Dockerfile`), `docker-compose*.yml`, Nginx config in `nginx/`, DB init in `init-scripts/`.
- Scripts and SQL/data utilities live at repo root (`*.sql`, `scripts/`, `deploy.sh`).

## Build, Test, and Development Commands
- Backend dev: `mvn spring-boot:run` (profile via `-Dspring-boot.run.profiles=dev|test|prod`).
- Backend build: `mvn clean package` (jar in `target/`).
- Backend tests: `mvn test` (JUnit + Testcontainers).
- Frontend dev: `cd frontend && npm install && npm run dev`.
- Frontend build/start: `cd frontend && npm run build && npm start`.
- E2E tests: `cd frontend && npm run test:e2e` (ensure app is running or use `docker-compose up -d`).
- Full stack (docker): `docker-compose up -d` for local services.

## Coding Style & Naming Conventions
- Java: 4‑space indent, package `com.codetop...`, classes `PascalCase`, methods/fields `camelCase`. Prefer constructor injection and Spring annotations.
- TypeScript/React: 2‑space indent, components `PascalCase` (e.g., `NoteEditor.tsx`), files `kebab-case` for utilities, colocate styles.
- Linting: Frontend uses ESLint (`frontend/eslint.config.mjs`). Run `npm run lint` in `frontend/`.

## Testing Guidelines
- Java: JUnit 5 with Spring Boot test utilities; use integration tests for controllers/services; name tests `*Test.java` mirroring package paths.
- Test data: prefer factories or `src/test/resources/*.sql` over hardcoding.
- Frontend: Playwright for E2E in `frontend/tests`. Keep tests idempotent; tag slow suites if needed.

## Commit & Pull Request Guidelines
- Commits: use Conventional Commits (`feat:`, `fix:`, `chore:`, `test:`, `docs:`). Example: `feat(api): add rate limit filter`.
- PRs: include purpose, linked issue (e.g., `Closes #123`), screenshots for UI, and testing notes. Keep PRs focused and under ~400 LOC when possible.

## Security & Configuration Tips
- Config: use profiles in `application.yml` and `.env` files (`.env.example` provided). Never commit secrets.
- Data: large SQL/json helpers live at root; do not modify production dumps in PRs.
- Logs: avoid committing generated logs; update `.gitignore` if new ones appear.
