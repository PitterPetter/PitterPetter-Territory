# 💌 Loveventure Territory Service

PitterPetter Territory Service는 러브벤처 커플이 방문한 지역을 해금하고 추적하는 백엔드 마이크로서비스입니다. 서울·경기 전역의 행정 경계를 PostGIS로 관리하고, JWT 기반 인증과 외부 Auth 서비스 연동으로 커플별 데이터 격리를 보장합니다.

## 📌 서비스 개요
- 좌표 기반으로 사용자의 현재 지역을 판별하고 커플이 해당 지역을 해금했는지 확인
- 초기 해금(Init)과 보상 해금(Reward)을 구분하여 티켓 소모, Redis 검증, Auth 연동을 처리
- GeoJSON 데이터를 PostgreSQL/PostGIS에 적재하여 경계 정보 및 중심 좌표를 제공
- 커플별 해금 현황을 시/도 → 구/군 구조로 집계하여 대시보드 화면과 GeoJSON 맵을 모두 지원
- Spring Cloud Config와 API Gateway의 보호 아래 마이크로서비스 아키텍처로 운용

## 🚀 주요 기능
### 🧭 지역 탐색 & 판별
- `GET /api/regions/check`: 커플 ID로 현재 좌표가 해금된 지역인지 확인 (`UNLOCKED_REGION`, `LOCKED_REGION`, `OUT_OF_COVERAGE`).
- `GET /api/regions/lookup`: 인증 없이 좌표가 포함된 행정구역을 조회.
- 좌표 검증 유틸(`ValidationUtils`)로 위경도 범위 오류 방지.

### 🔓 지역 해금 플로우
- `POST /api/regions/unlock/init`: Auth 서버 검증 → 티켓 차감 → 다중 지역 해금.
- `POST /api/regions/unlock/reward`: Gateway 티켓 검증(향후 Redis) → 다중 지역 해금.
- `UnlockService#unlockMultipleRegions`가 다중 요청을 단일 트랜잭션으로 처리하고 캐시(`unlockedRegions`)를 자동 무효화.

### 📡 해금 현황 제공
- `GET /api/regions/search`: 시/도별 집계 정보(`CitySummary`, `DistrictSummary`) 반환.
- `format=feature` 파라미터로 GeoJSON `FeatureCollection` 응답.
- 중심 좌표(`lat/lng`)와 설명 필드로 프런트의 지도 표시 및 리스트 UI 모두 지원.

### 🗺 Geo 데이터 관리
- `PostgisTestRunner`가 부팅 시 `sgg_seoul_gyeonggi.json`을 읽어 PostGIS 테이블을 생성/업데이트.
- GeoJSON → MultiPolygon 변환 후 `ST_MakeValid`, `ST_Multi`를 통해 정합성 유지.
- 좌표 검색은 `RegionRepository#findRegionByPoint`에서 `ST_Contains`로 수행.

### 🔐 보안 및 인증
- `CoupleHeaderResolver`가 JWT 서명(HMAC-SHA256)과 만료를 검증, 다양한 claim 키(`coupleId`, `couple_id` 등)를 지원.
- Local 프로필(`CoupleHeaderResolverLocal`)은 Swagger/Postman 테스트용 헤더 `COUPLE-ID`를 허용.
- `AuthClient`(OpenFeign)가 Auth 서비스의 토큰 검증 & 티켓 차감을 담당.
- 커플별 데이터는 `couple_region` 테이블에서 `(couple_id, region_id)` UNIQUE 제약으로 격리.

### 🧰 운영 편의 기능
- CORS 전역 허용(`WebConfig`), Swagger(OpenAPI 3) 문서 자동 생성.
- Spring Cache 추상화로 해금 목록 캐시(현재 `ConcurrentMapCacheManager`; 실 Redis 서비스는 `RedisTicketService` 도입 예정).
- Actuator 포함으로 헬스 체크 및 메트릭 노출.

## 🛠 기술 스택
| 영역 | 사용 기술 |
| --- | --- |
| 언어 & 런타임 | Java 17, Gradle 8 | 
| 프레임워크 | Spring Boot 3.4.10, Spring Data JPA, Spring Validation |
| 데이터베이스 | PostgreSQL 15 + PostGIS 3 |
| 캐시 & 메시징 | Spring Cache (ConcurrentMap) → Redis 마이그레이션 예정 |
| 외부 연동 | Spring Cloud OpenFeign, Spring Cloud Config |
| 보안 | JWT (jjwt 0.11.5), OAuth2 Resource Server 연동 (Auth 서비스) |
| 문서화 | SpringDoc OpenAPI 3, Swagger UI |
| 기타 | Jackson, Lombok, Dotenv, Hibernate Spatial |

## 🏗️ 아키텍처 설계
```
API Gateway (8080)
      ↓
Territory Service (8084, Docker 기본)
      ↓                 ↓
Auth Service        Config Server
      ↓                 ↓
PostgreSQL/PostGIS   Redis (planned)
```
- `spring.config.import=optional:configserver:`로 중앙 설정 로딩.
- Kubernetes 상에서는 `course-service`와 동일 네임스페이스의 Config Server & Auth 서비스와 통신.
- Docker 컨테이너는 `SERVER_PORT=8084`, JVM Option으로 메모리 제한 대응.

## 🧱 도메인 모델
```
CoupleRegion (커플-지역 관계)
  ↕ N:1
Region (행정 구역, MultiPolygon)
```
- `Region`: `sig_cd`(행정코드) UNIQUE, `geom`에 SRID 4326 MultiPolygon 저장.
- `CoupleRegion`: 잠금 여부(`is_locked`), 해금 시각(`unlocked_at`) 저장, `BaseEntity`로 생성/수정 이력 자동 관리.
- DTO 계층 (`RegionSummary`, `CitySummary`, `DistrictSummary`, `UnlockResponse` 등)으로 API 응답을 명확히 모델링.

## ⚙️ 환경 설정
### Spring Profile
- `prod`(기본): Config Server 연동, 실제 Auth/DB 사용.
- `local`: `.env` 또는 환경변수 기반으로 DB/Secret 주입, `CoupleHeaderResolverLocal` 활성화.

### 주요 환경 변수
| 키 | 설명 | 기본값 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | 실행 프로필 | `prod` |
| `JWT_SECRET` | JWT 서명 키(Base64 권장) | 필수 |
| `AUTH_SERVICE_URL` | Auth 서비스 베이스 URL | `http://localhost:8081` |
| `SERVER_PORT` | 애플리케이션 포트 | `8084` (Docker) |
| `SPRING_DATASOURCE_*` | Postgres 접속 정보 | local 프로필 참고 |

`.env` 예시 (local):
```dotenv
SPRING_PROFILES_ACTIVE=local
JWT_SECRET=ZmFrZS1zZWNyZXQ=
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/territory
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
AUTH_SERVICE_URL=http://localhost:8081
```

### Config Server 연동 (`application.yml`)
```yaml
spring:
  config:
    import: "optional:configserver:"
  cloud:
    config:
      uri: http://config-server.config-server.svc.cluster.local:80
      name: territory-service
      label: main
      fail-fast: false
```

## ▶ 실행 방법
### 로컬 개발 (PostgreSQL + PostGIS 필요)
```bash
# 의존성 다운로드 및 빌드
./gradlew build

# 로컬 프로필로 실행 (JWT/DB 환경변수 필요)
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```
- 최초 실행 시 `PostgisTestRunner`가 `sgg_seoul_gyeonggi.json`을 읽어 `region` 데이터를 적재 (PostGIS extension 필요).
- Swagger: `http://localhost:8080/swagger-ui.html` (local), `http://localhost:8084/swagger-ui.html` (Docker).

### Docker
```bash
# 이미지 빌드
docker build -t pitterpetter-territory-service .

# 컨테이너 실행 (Config Server & Auth 네트워크 필요)
docker run -d \
  --name territory-service \
  --network microservices-network \
  -p 8084:8084 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JWT_SECRET=ZmFrZS1zZWNyZXQ= \
  -e AUTH_SERVICE_URL=http://auth-service:8081 \
  pitterpetter-territory-service
```

### Kubernetes 배포 스니펫
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: territory-service
  namespace: microservices
spec:
  replicas: 3
  selector:
    matchLabels:
      app: territory-service
  template:
    metadata:
      labels:
        app: territory-service
    spec:
      containers:
        - name: territory-service
          image: pitterpetter-territory-service:latest
          ports:
            - containerPort: 8084
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: SERVER_PORT
              value: "8084"
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: territory-secrets
                  key: jwt-secret
```

## 📡 API 엔드포인트 요약
### 내부 서비스 (기본 포트 8080/8084)
| Method | Endpoint | 설명 | 인증 |
| --- | --- | --- | --- |
| GET | `/api/regions/check` | 좌표가 해금된 지역인지 판별 | ✅ (JWT) |
| GET | `/api/regions/lookup` | 좌표가 속한 행정구역 조회 | ❌ |
| GET | `/api/regions/status` | 커플 ID 추출 상태 확인 (디버그) | ✅ |
| GET | `/api/regions/search?format=list` | 커플별 해금 현황 요약 | ✅ |
| GET | `/api/regions/search?format=feature` | 해금 지역 GeoJSON | ✅ |
| POST | `/api/regions/unlock/init` | Auth 검증 + 티켓 차감 + 해금 | ✅ |
| POST | `/api/regions/unlock/reward` | Redis 티켓 검증 + 해금 | ✅ |

### Gateway 연동 시 예상 경로
| Method | Gateway Endpoint | 내부 매핑 |
| --- | --- | --- |
| GET | `/territory/api/regions/**` | Territory Service (8084) |
| POST | `/territory/api/regions/unlock/**` | Territory Service (8084) |

### Swagger & OpenAPI
- 내부: `GET /swagger-ui.html`, `GET /v3/api-docs`
- Gateway: `http://api-gateway:8080/territory/swagger-ui.html`

## 🗄️ 데이터베이스 설계
### `region`
| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | VARCHAR | 기본 키 (기본적으로 `sig_cd`) |
| `sig_cd` | VARCHAR(10) | 행정 코드, UNIQUE |
| `si_do` | VARCHAR | 시/도 이름 |
| `gu_si` | VARCHAR | 구/군 이름 |
| `geom` | geometry(MultiPolygon,4326) | 행정 경계 |

### `couple_region`
| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | BIGSERIAL | PK |
| `couple_id` | VARCHAR | 커플 식별자 |
| `region_id` | VARCHAR | `region.id` FK |
| `is_locked` | BOOLEAN | 잠금 여부 (기본 true) |
| `unlocked_at` | TIMESTAMP | 해금 시각 |
- UNIQUE(`couple_id`, `region_id`)로 중복 해금 방지.
- JPA Auditing(`BaseEntity`)로 `created_at`, `updated_at` 관리.

### 인덱스 & 성능 메모
- `region(sig_cd)` UNIQUE + 조회 인덱스.
- 좌표 검색은 PostGIS `ST_Contains` + GiST 인덱스 권장.
- `couple_region(couple_id, is_locked)` 복합 인덱스로 해금 현황 조회 최적화 가능.

## 🔧 개발 가이드
### 패키지 구조
```
src/main/java/com/pitterpetter/loventure/territory
├── api/                # REST 컨트롤러
├── application/        # 서비스 계층 (비즈니스 로직)
├── config/             # Swagger, Redis(Cache), WebConfig
├── domain/             # 엔티티 및 리포지토리
│   ├── common/         # 공통 베이스 엔티티
│   ├── coupleregion/   # 커플-지역 도메인
│   └── region/         # 지역 도메인
├── dto/                # 요청/응답 DTO 모음
├── exception/          # 예외 및 핸들러
├── infra/              # 외부 서비스(Feign)
└── util/               # JWT, GeoJSON, Validation 유틸
```
- DDD 기반으로 API → Application → Domain 순의 의존성 방향 유지.
- 컨트롤러에서 DTO 변환 및 유효성 검증, 서비스 계층에서 트랜잭션 관리.

### 코딩 규칙
- Lombok `@Builder`, `@NoArgsConstructor` 등으로 보일러플레이트 최소화.
- 서비스 메서드는 `@Transactional` + 읽기 전용(`readOnly=true`) 구분.
- 예외는 `ApiException(ErrorCode)`로 통일해 ResponseEntity에 반영.

## 🧪 테스트
```bash
# 기본 단위 테스트
./gradlew test
```
- 현재 통합 테스트 템플릿은 미구성. PostGIS 종속성이 있어 Testcontainers 도입 예정.
- `PostgisTestRunner`는 실행 시 실제 DB를 수정하므로 테스트 환경에서는 비활성화 필요.

## 📊 모니터링 & 로깅
- Spring Boot Actuator 제공: `/actuator/health`, `/actuator/info` 등.
- 해금 시나리오별 로깅(`UnlockController`, `UnlockService`)으로 장애 추적.
- 추후 Prometheus/Grafana 연동 시 `application.yaml`에서 지표 exporter 추가 예정.

## 🚀 배포 전략
1. 컨테이너 이미지 빌드 및 레지스트리 푸시.
2. Config Server 및 Auth 서비스가 준비된 네트워크에 배포.
3. PostGIS가 활성화된 PostgreSQL 인스턴스와 Redis 인스턴스 준비.
4. Helm/Kustomize로 `Deployment`, `Service`, `Secret` 템플릿 관리 권장.

## 🧩 브랜치 전략
- `main`: 배포 가능한 안정 버전.
- `develop`: 통합 개발 브랜치.
- `feature/PIT-이슈번호`: 기능 단위 개발.
- `hotfix/PIT-이슈번호`: 긴급 수정.

## 📜 커밋 규칙
- `feat`: 새로운 기능 추가.
- `fix`: 버그 수정.
- `docs`: 문서 수정 (README 포함).
- `refactor`: 기능 변화 없는 구조 개선.
- `test`: 테스트 추가/보완.
- `perf`: 성능 개선.
- `chore`: 빌드, 설정, 기타 잡무.
