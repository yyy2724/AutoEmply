# AutoEmply

AutoEmply is split into:

- `AutoEmply-server`: Spring Boot API that validates `LayoutSpec`, generates Delphi QuickReport `dfm/pas`, persists prompt presets and report templates, and runs Flyway migrations.
- `AutoEmply-client`: React + Vite client for image/PDF driven generation, JSON export, prompt management, and template browsing.

## Requirements

- Java 21
- Node.js 20+
- PostgreSQL 15+ for local server runs

## Server

From `AutoEmply-server`:

```bash
./gradlew bootRun
```

Important environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `ANTHROPIC_API_KEY`
- `ANTHROPIC_MODEL` optional

Flyway runs automatically on startup. The initial schema lives in `src/main/resources/db/migration/V1__initial_schema.sql`.

Run tests:

```bash
./gradlew test
```

## Client

From `AutoEmply-client`:

```bash
npm install
npm run dev
```

Optional environment variables:

- `VITE_API_BASE_URL` default `http://localhost:8080`
- `VITE_API_TIMEOUT_MS` default `150000`

## Main APIs

- `POST /api/export`
- `POST /api/generate-json`
- `POST /api/export-from-image`
- `GET /api/prompts`
- `POST /api/prompts`

## Notes

- Server integration tests run on H2 in PostgreSQL compatibility mode with Flyway enabled.
- Delphi generator regression tests cover the C# parity corrections that were ported into Java.
