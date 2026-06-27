# AutoEmply

양식 이미지(또는 PDF)를 AI로 분석해 **Delphi QuickReport 리포트 소스(`.dfm` / `.pas`)를 자동 생성**하는 도구입니다.

업로드된 양식을 Claude API로 구조 분석하여 `LayoutSpec` JSON으로 변환하고, 이를 검증·후처리한 뒤 Delphi 소스 코드로 출력합니다. 프롬프트 프리셋과 생성된 템플릿은 PostgreSQL에 저장되어 재사용할 수 있습니다.

## 구성

| 모듈 | 기술 스택 | 역할 |
|---|---|---|
| `AutoEmply-server` | Java 21 · Spring Boot · PostgreSQL · Flyway | LayoutSpec 검증, Delphi dfm/pas 생성, 프롬프트 프리셋·리포트 템플릿 관리, Claude API 연동 |
| `AutoEmply-client` | React · TypeScript · Vite · Mantine | 이미지/PDF 업로드 기반 생성, JSON 내보내기, 프롬프트 관리, 템플릿 라이브러리 |

처리 흐름:

```
이미지/PDF 업로드 → Claude API 구조 분석 → LayoutSpec JSON
  → 검증(LayoutSpecValidator) → 후처리(LayoutPostProcessor)
  → Delphi 코드 생성(DelphiGenerator) → dfm/pas 다운로드
```

## 요구 사항

- Java 21
- Node.js 20+
- PostgreSQL 15+ (로컬 서버 실행 시)
- Anthropic API 키

## 실행 방법

### 1. PostgreSQL 준비

`autoemply` 데이터베이스를 생성합니다. 스키마는 서버 기동 시 Flyway가 자동으로 마이그레이션합니다
(`AutoEmply-server/src/main/resources/db/migration/`).

### 2. 서버 실행

```bash
cd AutoEmply-server
./gradlew bootRun
```

환경 변수:

| 변수 | 필수 | 설명 |
|---|---|---|
| `DB_URL` | ✅ | 예: `jdbc:postgresql://localhost:5432/autoemply` |
| `DB_USERNAME` | ✅ | DB 사용자 |
| `DB_PASSWORD` | ✅ | DB 비밀번호 |
| `ANTHROPIC_API_KEY` | ✅ | Claude API 키 |
| `ANTHROPIC_MODEL` | — | 기본 모델 재정의 (선택) |

### 3. 클라이언트 실행

```bash
cd AutoEmply-client
npm install
npm run dev
```

기본 접속 주소: `http://localhost:5173`

환경 변수 (선택):

| 변수 | 기본값 | 설명 |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080` | 서버 API 주소 |
| `VITE_API_TIMEOUT_MS` | `150000` | API 타임아웃 (ms) |

## 테스트

```bash
# 서버 — H2(PostgreSQL 호환 모드) + Flyway 기반 통합 테스트 포함
cd AutoEmply-server && ./gradlew test

# 클라이언트 — 타입 검사 및 프로덕션 빌드
cd AutoEmply-client && npm run typecheck && npm run build
```

Delphi 생성기 회귀 테스트는 C# 원본 구현과의 동작 일치(parity)를 검증합니다.

## 주요 API

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/export` | LayoutSpec JSON → dfm/pas 생성 |
| `POST` | `/api/generate-json` | 이미지/PDF → LayoutSpec JSON 생성 |
| `POST` | `/api/export-from-image` | 이미지/PDF → dfm/pas 일괄 생성 |
| `POST` | `/api/generate-structure` | 이미지/PDF → 양식 구조 분석 |
| `GET/POST/PUT/DELETE` | `/api/prompts` | 프롬프트 프리셋 관리 |
| `GET/POST/DELETE` | `/api/report-templates` | 리포트 템플릿 관리 (미리보기·다운로드 포함) |
| `GET/POST/PUT/DELETE` | `/api/sample-template-sets` | 샘플 템플릿 세트 관리 |
| `GET/PUT` | `/api/ai-version` | 사용 중인 AI 모델 조회/변경 |

## 배포

`master` 브랜치에 push하면 GitHub Actions가 자동 배포합니다 (`.github/workflows/`).

- **서버**: Gradle `bootJar` 빌드 → EC2 전송 → systemd 서비스(`autoemply`) 재시작
- **클라이언트**: Vite 빌드 → S3(`autoemply-client`) 동기화 → CloudFront 캐시 무효화

## 프로젝트 구조

```
AutoEmply-server/src/main/java/health/autoemplyserver/
├── controller/   # REST API 엔드포인트
├── application/  # 유스케이스 단위 애플리케이션 서비스
├── service/      # 핵심 도메인 로직 (Claude 연동, Delphi 생성, 레이아웃 후처리)
├── model/        # LayoutSpec 등 레이아웃 도메인 모델
├── entity/       # JPA 엔티티
├── repository/   # Spring Data 리포지토리
├── dto/          # 요청/응답 DTO
└── config/       # 설정 (CORS, AI 속성, 환경변수 브리지)

AutoEmply-client/src/
├── pages/        # 라우트별 페이지 (생성, 라이브러리, 프리셋)
├── hooks/        # 페이지별 상태/비즈니스 로직 훅
├── features/     # 도메인별 API 호출 모듈
├── components/   # 공용 UI 컴포넌트
└── lib/          # API 클라이언트, 테마, 유틸리티
```
