# auth-api

Spring Boot 기반 JWT 인증 실습 프로젝트입니다.  
Access Token / Refresh Token 발급, Redis 기반 토큰 관리, Spring Security 커스터마이징, 분산 추적(RequestId 전파) 등의 인증 아키텍처를 직접 구현하며 학습하는 것을 목적으로 합니다.

---

## 기술 스택

| 분류 | 기술 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5.7 |
| Security | Spring Security 6, JJWT 0.12.5 |
| Database | H2 (In-Memory, 로컬 개발용) |
| Cache / Token Store | Redis (Lettuce) |
| ORM | Spring Data JPA, Hibernate 6, QueryDSL 5 |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Resilience | Resilience4j (Circuit Breaker, Retry) |
| Monitoring | Spring Actuator, Micrometer |
| Build | Gradle |

---

## 프로젝트 구조

```
src/main/java/com/okbank/fintech/
├── FundaApiApplication.java
├── domain/
│   ├── auth/
│   │   ├── controller/   # AuthController (로그인, 토큰 재발급, 로그아웃)
│   │   ├── dto/          # LoginRequest, RefreshTokenRequest, TokenResponse, UserResponse
│   │   └── service/      # AuthService, RefreshTokenService
│   └── user/
│       ├── controller/   # UserController (회원가입, 내 정보 조회, 회원 탈퇴)
│       ├── dto/          # UserCreateRequest
│       ├── entity/       # Member, UserRole, UserStatus
│       ├── repository/   # MemberRepository
│       └── service/      # UserService
└── global/
    ├── common/           # BaseEntity, BaseTimeEntity, DataResponse, ErrorResponse
    ├── config/           # Security, JPA, Swagger, Async, WebClient, WebConfig
    ├── exception/        # GlobalExceptionHandler, BusinessException 계층
    ├── filter/           # JwtAuthenticationFilter, LoggingFilter
    ├── interceptor/      # LoggingInterceptor, RequestIdPropagationInterceptor
    ├── security/         # JwtTokenProvider, CustomUserDetails(Service), EntryPoint, AccessDeniedHandler
    └── util/             # RequestIdPropagator (MDC 기반)
```

---

## 주요 구현 내용

### 1. JWT 인증 흐름

```
[POST /api/auth/login]
  → AuthenticationManager.authenticate()
  → CustomUserDetailsService.loadUserByUsername()
  → JwtTokenProvider.createAccessToken() / createRefreshToken()
  → RefreshTokenService.save() → Redis 저장 (key: RT:{mobile})
  → TokenResponse 반환 (accessToken + refreshToken)
```

- **Access Token**: HS256 서명, 유효기간 1시간 (`jwt.access-token-validity`)
- **Refresh Token**: HS256 서명, 유효기간 7일 (`jwt.refresh-token-validity`), Redis에 저장
- **토큰 재발급** (`/api/auth/refresh`): Refresh Token 검증 → Redis 저장 토큰과 비교 → 신규 토큰 쌍 발급 (Rotation)
- **로그아웃** (`/api/auth/logout`): Redis에서 Refresh Token 삭제 + SecurityContext 초기화

### 2. JwtAuthenticationFilter

`OncePerRequestFilter`를 확장하여 모든 요청에서 JWT를 검사합니다.

1. `Authorization: Bearer {token}` 헤더에서 토큰 추출
2. `JwtTokenProvider.validateToken()` — 서명 검증, 만료 확인
3. Access Token 타입 여부 확인 (`claim: type=access`)
4. `CustomUserDetails` 로드 후 사용자 상태(`UserStatus`) 확인
5. `SecurityContextHolder`에 Authentication 저장

### 3. Spring Security 설정

- **Stateless 세션** (`SessionCreationPolicy.STATELESS`)
- **CSRF 비활성화** (JWT 기반이므로)
- **DaoAuthenticationProvider** + `CustomUserDetailsService` + `BCryptPasswordEncoder`
- **공개 엔드포인트**: `/api/auth/login`, `/api/auth/refresh`, `/api/users/signup`, Swagger UI, H2 Console
- **예외 처리**: `JwtAuthenticationEntryPoint` (401), `JwtAccessDeniedHandler` (403)

### 4. 사용자 상태 관리

```java
public enum UserStatus {
    ACTIVE, INACTIVE, SUSPENDED, WITHDRAWN
}
```

로그인, 토큰 재발급, 필터 단계 모두에서 `UserStatus`를 확인하며, `ACTIVE`가 아닌 경우 인증을 차단합니다.

### 5. RequestId 기반 분산 추적

- `LoggingFilter`: 요청 수신 시 `X-Request-Id` 헤더를 읽거나 UUID를 생성하여 MDC에 저장, 응답 헤더에 포함
- `RequestIdPropagator`: MDC 기반 RequestId 관리, Kafka 헤더 전파/복원 지원
- `RequestIdPropagationInterceptor`: WebClient / RestTemplate 외부 호출 시 `X-Request-Id` 헤더 자동 전파
- `GlobalExceptionHandler`: 에러 응답에 `requestId` 포함

### 6. 글로벌 예외 처리

`@RestControllerAdvice`로 일관된 `ErrorResponse` 반환:

| 예외 | HTTP Status |
|---|---|
| `BusinessException` | 정의된 상태 코드 |
| `MethodArgumentNotValidException` | 400 |
| `BadCredentialsException` | 401 |
| `UnauthorizedException` | 401 |
| `AccessDeniedException` | 403 |
| `DisabledException` | 403 |
| `UsernameNotFoundException` | 401 |
| 그 외 `Exception` | 500 |

---

## API 명세

### Auth

| Method | URL | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/auth/login` | 불필요 | 로그인 (Access + Refresh Token 발급) |
| POST | `/api/auth/refresh` | 불필요 | Refresh Token으로 토큰 재발급 |
| POST | `/api/auth/logout` | 필요 | 로그아웃 (Redis Refresh Token 삭제) |

### User

| Method | URL | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/users/signup` | 불필요 | 회원가입 |
| GET | `/api/users/me` | 필요 | 내 정보 조회 |
| DELETE | `/api/users/withdraw` | 필요 | 회원 탈퇴 |

> Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## 실행 방법

### 사전 요구사항

- Java 17+
- Redis (로컬 실행, 기본 포트 6379)

### 로컬 실행

```bash
# Redis 실행 (예: Docker)
docker run -d -p 6379:6379 redis

# 애플리케이션 실행
./gradlew bootRun
```

### 주요 설정 (`application.yaml`)

```yaml
jwt:
  secret-key: bXktc3VwZXItc2VjcmV0LWtleS1tdXN0LWJlLWxvbmc=
  access-token-validity: 3600    # 1시간 (초)
  refresh-token-validity: 604800 # 7일 (초)

spring:
  datasource:
    url: jdbc:h2:mem:test        # In-Memory H2
  data:
    redis:
      host: localhost
      port: 6379
```

> H2 콘솔: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:test`)

---

## 학습 포인트

- Spring Security 6 커스터마이징 (`SecurityFilterChain`, `AuthenticationProvider`, `OncePerRequestFilter`)
- JWT 발급 / 검증 / 타입 구분 (`access` / `refresh` claim)
- Redis 기반 Refresh Token 저장 및 Rotation 전략
- MDC 기반 RequestId 전파 (HTTP → Kafka → 외부 API)
- `@RestControllerAdvice`를 활용한 글로벌 예외 처리 및 `requestId` 포함 에러 응답
