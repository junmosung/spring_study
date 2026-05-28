# 01-mvc-basics

Spring Boot 4 + Kotlin 환경에서 Spring MVC의 핵심을 단계적으로 익히는 첫 학습 모듈.
루트 [`docs/learning-roadmap.md`](../docs/learning-roadmap.md)의 **Tier 1 전체**(IoC/DI, MVC 요청 흐름, Validation, Configuration, Logging, JPA 기초)를 다룬다.

## 스택

| 영역 | 사용 기술 |
|---|---|
| 언어 / JDK | Kotlin 2.2.21 / JDK 24 |
| 프레임워크 | Spring Boot 4.0.6 (Spring Framework 7) |
| 서블릿 컨테이너 | **Jetty 12** (embedded, Tomcat 대신) |
| DB / 마이그레이션 | PostgreSQL 17 + Flyway |
| 검증 / 에러 응답 | Jakarta Bean Validation + ProblemDetail (RFC 9457) |
| JSON | Jackson + `jackson-module-kotlin` |

스택 결정의 배경은 [`docs/01-foundation/`](./docs/01-foundation/)에서 다룬다.

## Quick Start

루트에서 공유 인프라(postgres)를 기동한 뒤 모듈만 실행:

```bash
# 루트에서 1회만
cp .env.example .env
docker compose up -d postgres

# 실행
set -a; . ./.env; set +a
./gradlew :01-mvc-basics:bootRun

# 확인
curl http://localhost:8080/ping
```

전체 절차·트러블슈팅은 루트 [`docs/getting-started.md`](../docs/getting-started.md) 참고.

## 문서 (`docs/`)

학습 단계별 3개 카테고리로 분류.

### [`01-foundation/`](./docs/01-foundation/) — 환경 · 런타임 · 아키텍처
- [`architecture.md`](./docs/01-foundation/architecture.md) — 모듈 전체 구조, 부팅 순서, Flyway 규칙
- [`setup-verification.md`](./docs/01-foundation/setup-verification.md) — 초기 환경 검증 체크리스트
- [`jetty-vs-tomcat.md`](./docs/01-foundation/jetty-vs-tomcat.md) — 왜 Tomcat 대신 Jetty 12를 골랐는지

### [`02-ioc/`](./docs/02-ioc/) — IoC / DI / Bean / 외부 설정 바인딩
- [`bean-registration-and-di.md`](./docs/02-ioc/bean-registration-and-di.md) — `@Component`·`@Bean`·생성자 주입·`@Primary`/`@Qualifier`
- [`bean-scope-and-lifecycle.md`](./docs/02-ioc/bean-scope-and-lifecycle.md) — Scope, `@PostConstruct`, `BeanPostProcessor`
- [`configuration-properties.md`](./docs/02-ioc/configuration-properties.md) — `@ConfigurationProperties` + Kotlin data class + Validation

### [`03-mvc/`](./docs/03-mvc/) — 요청 처리 / Filter / Interceptor / Validation
- [`mvc-request-flow.md`](./docs/03-mvc/mvc-request-flow.md) — DispatcherServlet 흐름, 컨버터, `ResponseEntity`, Content Negotiation
- [`filter-vs-interceptor.md`](./docs/03-mvc/filter-vs-interceptor.md) — 한눈 비교 + 결정 트리
- [`filter-vs-interceptor-deep-dive.md`](./docs/03-mvc/filter-vs-interceptor-deep-dive.md) — `doFilter` 타이밍 + DispatcherServlet 7단계 메커니즘
- [`validation-and-problem-detail.md`](./docs/03-mvc/validation-and-problem-detail.md) — Jakarta Validation + `@RestControllerAdvice` + RFC 9457

## 학습 예제 코드

각 학습 주제는 `src/main/kotlin/me/victor/spring01/` 아래 별도 패키지에 격리돼 있어, 한 주제를 골라 따라가기 좋다.

| 패키지 | 주제 | 짝 문서 |
|---|---|---|
| [`learning/ioc/`](./src/main/kotlin/me/victor/spring01/learning/ioc/) | `@Service`·`@Bean`·생성자 주입·다중 후보(`@Primary`/`@Qualifier`) | [`02-ioc/bean-registration-and-di.md`](./docs/02-ioc/bean-registration-and-di.md) |
| [`learning/lifecycle/`](./src/main/kotlin/me/victor/spring01/learning/lifecycle/) | Singleton/Prototype, 라이프사이클 훅, `BeanPostProcessor` | [`02-ioc/bean-scope-and-lifecycle.md`](./docs/02-ioc/bean-scope-and-lifecycle.md) |
| [`learning/config/`](./src/main/kotlin/me/victor/spring01/learning/config/) | `@ConfigurationProperties` + 중첩/컬렉션/Map/Duration/DataSize | [`02-ioc/configuration-properties.md`](./docs/02-ioc/configuration-properties.md) |
| [`learning/mvc/`](./src/main/kotlin/me/victor/spring01/learning/mvc/) | `@RestController`, 모든 파라미터 어노테이션, Content Negotiation, `ResponseEntity` | [`03-mvc/mvc-request-flow.md`](./docs/03-mvc/mvc-request-flow.md) |
| [`learning/validation/`](./src/main/kotlin/me/victor/spring01/learning/validation/) | `@Valid` 4가지 검증 트리거 + ProblemDetail 변환 | [`03-mvc/validation-and-problem-detail.md`](./docs/03-mvc/validation-and-problem-detail.md) |
| [`web/filter/`](./src/main/kotlin/me/victor/spring01/web/filter/) | `OncePerRequestFilter` 요청/응답 로깅 | [`03-mvc/filter-vs-interceptor*.md`](./docs/03-mvc/) |
| [`web/interceptor/`](./src/main/kotlin/me/victor/spring01/web/interceptor/) | `HandlerInterceptor`로 핸들러 단위 metric | [`03-mvc/filter-vs-interceptor*.md`](./docs/03-mvc/) |

## 다음 단계

이 모듈을 마치면 다음 모듈 `02-data-jpa`로 이어가 Tier 2 #7·8·14(JPA 심화 + Transaction + Application Events)를 학습. 전체 진행 순서는 루트 로드맵의 [추천 모듈 진행 순서](../docs/learning-roadmap.md#추천-모듈-진행-순서) 섹션 참고.
