# 빠른 실행 가이드

상세한 설명(환경 변수 전체 목록, API, 배포)은 [README.md](README.md)를 참고하세요.

## 1. PostgreSQL 시작

`autoemply` 데이터베이스를 생성합니다.

## 2. 서버 실행

```bash
cd AutoEmply-server
./gradlew bootRun
```

환경 변수 예시:

```bash
DB_URL=jdbc:postgresql://localhost:5432/autoemply
DB_USERNAME=postgres
DB_PASSWORD=postgres
ANTHROPIC_API_KEY=your-key
```

## 3. 클라이언트 실행

```bash
cd AutoEmply-client
npm install
npm run dev
```

접속 주소: `http://localhost:5173`

## 4. 테스트

```bash
# 서버
cd AutoEmply-server
./gradlew test

# 클라이언트
cd AutoEmply-client
npm run typecheck
npm run build
```
