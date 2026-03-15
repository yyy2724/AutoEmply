# Run Guide

## 1. Start PostgreSQL

Create a database named `autoemply`.

## 2. Run the server

```bash
cd AutoEmply-server
./gradlew bootRun
```

Example environment:

```bash
DB_URL=jdbc:postgresql://localhost:5432/autoemply
DB_USERNAME=postgres
DB_PASSWORD=postgres
ANTHROPIC_API_KEY=your-key
```

## 3. Run the client

```bash
cd AutoEmply-client
npm install
npm run dev
```

Client default URL: `http://localhost:5173`

## 4. Test

Server:

```bash
cd AutoEmply-server
./gradlew test
```

Client:

```bash
cd AutoEmply-client
npm run build
```
