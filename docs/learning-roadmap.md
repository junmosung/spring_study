# Spring 학습 로드맵

`spring-study` 모노리포에서 단계적으로 학습할 개념·기술 우선순위. 현재 스택(Spring Boot 4.0.6 + Kotlin 2.2.21 + Java 24 + Jetty 12 + PostgreSQL)을 기준으로 작성.

> ⚠️ 주의: Spring Boot 4 / Spring Framework 7 / Spring Security 7은 매우 최신 라인이라 한국어 자료가 부족하다. Boot 3.x 자료를 참고할 때 deprecation/breaking change를 항상 교차 확인할 것.

---

## Tier 1 — 핵심 기본기 (`01-mvc-basics`에서 다룰 것)

체득하지 않으면 그 위의 어떤 기술도 표면적으로만 쓰게 된다.

### 1. IoC / DI / Bean 라이프사이클
- **왜**: Spring의 본질. `@Component`, `@Bean`, `@Configuration`, scope, 순환 의존성, `BeanPostProcessor`까지 이해해야 "왜 이게 동작하는가"를 설명할 수 있다.
- **무엇**: Constructor injection 강제, `@Primary` vs `@Qualifier`, `@ConfigurationProperties`, Kotlin `data class` + `@ConstructorBinding`.

### 2. Spring MVC 요청 흐름
- **왜**: `DispatcherServlet` → `HandlerMapping` → `HandlerAdapter` → `HttpMessageConverter`/`ViewResolver` 흐름을 모르면 디버깅이 미궁이다.
- **무엇**: `@RestController`, `@RequestMapping` 변형, Jackson(`jackson-module-kotlin` 이미 포함), content negotiation, `ResponseEntity`.

### 3. Validation + 예외 처리
- **왜**: 실무 API는 입력 검증과 일관된 에러 응답이 80%. 코드 품질 차이가 가장 크게 드러난다. `spring-boot-starter-validation` 이미 의존성에 포함됨.
- **무엇**: Jakarta Bean Validation(`@Valid`, `@Validated`), `@ControllerAdvice`, **`ProblemDetail` / RFC 9457** (Spring 6+ 표준).

### 4. Configuration & Profiles
- **왜**: 모듈마다 DB 분리, dev/test/prod 환경 분리. 이미 `.env` + Docker Compose 흐름을 쓰고 있어 자연스럽게 연결됨.
- **무엇**: `application-{profile}.yaml`, `@ConfigurationProperties` + Kotlin data class, `@Value` vs properties binding.

### 5. Logging 기본기
- **왜**: 관측 가능성의 출발점. Tier 3의 OpenTelemetry로 자연스럽게 확장된다.
- **무엇**: SLF4J + Logback, MDC, structured logging(JSON), log level 동적 변경.

### 6. JPA 기초 + Flyway (이미 classpath)
- **왜**: `01-mvc-basics`의 `build.gradle.kts`에 이미 `spring-boot-starter-data-jpa` + `flyway-core` + `flyway-database-postgresql`이 포함돼 있다. `src/main/resources/db/migration/.gitkeep`도 잡혀있음. 첫 모듈에서 한 도메인이라도 끝까지 흐르게 만드는 게 학습 효과가 크다.
- **무엇**: `@Entity` + Kotlin allOpen 플러그인(이미 설정), `JpaRepository` 기본, Flyway 마이그레이션 V1 작성, dirty checking.

---

## Tier 2 — 실무 필수 (다음 모듈부터)

여기까지 와야 "회사에서 굴러가는 Spring 코드"를 짤 수 있다.

### 7. JPA / Hibernate 심화
- **왜**: 영속성 컨텍스트, N+1, fetch 전략은 면접·실무의 단골 주제.
- **무엇**: `@EntityGraph`, JPQL/Criteria, 양방향 연관관계 함정, `@Modifying` + `@Query`. 복잡 쿼리는 **QueryDSL** 또는 **jOOQ**(Kotlin 친화·타입 안전) 비교 학습.

### 8. 트랜잭션 관리
- **왜**: `@Transactional` 한 줄 뒤에 propagation / isolation / 롤백 규칙이 다 숨어 있다. 자가 호출(self-invocation) 문제는 반드시 한 번 직접 부딪쳐야 한다.
- **무엇**: AOP 프록시 메커니즘과 함께 학습. `REQUIRED` vs `REQUIRES_NEW`, `readOnly` 최적화, 롤백 룰.

### 9. Spring Security
- **왜**: 인증 / 인가는 모든 서비스의 기본 요구사항. Spring Security **7**(Boot 4 기준)에서는 lambda DSL이 사실상 유일한 방식이라, 옛날 자료를 참고하면 오히려 헷갈린다.
- **무엇**:
  1. Filter Chain 흐름 이해
  2. Form login → 세션 기반
  3. **JWT vs 세션** 비교 (각각의 트레이드오프)
  4. **OAuth2 Resource Server / Client**
  5. Method security(`@PreAuthorize`)
- **팁**: 인증 흐름을 한 번 직접 그려보지 않으면 평생 검색만 하게 된다.

### 10. 테스트
- **왜**: 테스트 없는 학습은 "동작했다 = 정상이다"가 된다. Spring은 테스트 도구가 매우 풍부하다.
- **무엇**: `@SpringBootTest` vs `@WebMvcTest` vs `@DataJpaTest`, `MockMvc`, **Testcontainers**(이미 Postgres 쓰니 궁합 완벽), Kotest 또는 JUnit 5 + MockK.

### 11. Spring Boot Actuator + Micrometer
- **왜**: 모니터링 / 헬스체크 / 메트릭의 표준 입구. 운영의 출발점.
- **무엇**: `/actuator/health`, `/actuator/metrics`, Prometheus endpoint 노출, custom health indicator.

---

## Tier 3 — 신규 / 모던 기술 (Spring Boot 4 시대의 차별화)

Spring Boot 4를 쓰기로 한 이상 이게 진짜 재미있는 부분.

### 12. Virtual Threads (Project Loom)
- **왜**: Boot 3.2+부터 `spring.threads.virtual.enabled=true` 한 줄로 동시성 모델이 바뀐다. Java 24 + Jetty 12 조합이라 Loom 친화도가 매우 높다.
- **무엇**: 기존 blocking 코드 그대로 두고 처리량 끌어올리기. WebFlux 대안. 단, ThreadLocal pinning 같은 함정 학습.

### 13. Kotlin Coroutines + Spring
- **왜**: Kotlin을 쓰는 가장 큰 이유 중 하나. WebFlux의 reactive보다 코루틴이 가독성에서 압도적이다.
- **무엇**: `suspend` controller, `CoroutineScope` 통합, `kotlinx-coroutines-reactor`, Virtual Threads와의 역할 분담 정리.

### 14. HTTP Interface (선언적 HTTP 클라이언트)
- **왜**: Spring 6의 `@HttpExchange`. Feign 없이도 인터페이스만 선언하면 클라이언트가 생성된다. RestTemplate / WebClient 직접 쓰는 시대는 끝.
- **무엇**: `RestClient` + `@HttpExchange`.

### 15. Observability (OpenTelemetry / Micrometer Tracing)
- **왜**: Boot 3부터 Sleuth가 사라지고 Micrometer Tracing이 표준. 분산 환경 진입 전 반드시 익혀야 한다.
- **무엇**: trace / span 자동 전파, OTLP exporter, log–trace correlation.

### 16. GraalVM Native Image / AOT
- **왜**: Boot 3+ 공식 지원. 콜드 스타트가 수십 배 빨라진다. 서버리스 / CLI 배포에 매력.
- **무엇**: `./gradlew nativeCompile`, reflection 힌트, `@RegisterReflectionForBinding`.

### 17. Spring AI *(선택, 관심 있다면 강추)*
- **왜**: 2024년 1.0 GA. ChatModel / EmbeddingModel / VectorStore 추상화. 현재 가장 핫한 영역.
- **무엇**: RAG 파이프라인, tool calling, MCP 통합.

---

## Tier 4 — 분산 시스템 / MSA (관심 생기면)

18. **Spring Cloud Gateway** — API Gateway 표준.
19. **Messaging** — Spring for Apache Kafka, RabbitMQ. 비동기 아키텍처 필수.
20. **Resilience4j** — Circuit Breaker, Retry, Bulkhead.
21. **Spring Cloud Config / Consul / Eureka** — 분산 설정·서비스 디스커버리.

> 솔직히 말해, 회사에서 MSA를 안 한다면 Tier 4의 우선순위를 높일 필요는 없다. Tier 1~3을 깊게 가는 게 ROI가 훨씬 높다.

---

## Tier 5 — 심화 (Spring을 "쓰는 사람"이 아닌 "이해하는 사람"이 되기)

22. **AOP 내부 동작** — JDK Dynamic Proxy vs CGLIB, `@Transactional` / `@Async` 자가 호출 문제의 근본 원인.
23. **Auto-Configuration 직접 만들기** — `META-INF/spring/...AutoConfiguration.imports`, `@ConditionalOn*`, 커스텀 starter 작성.
24. **Servlet 스펙 + Jetty 12 내부** — 이미 Jetty로 갈아탔으니, EE10 namespace 전환과 Jetty의 virtual thread 모델까지 파면 차별화 포인트.
25. **Reactive Streams 자체** — WebFlux를 안 쓰더라도 backpressure 개념은 알아두면 좋다.

---

## 추천 모듈 진행 순서

```
01-mvc-basics       → Tier 1 전부 (+ JPA·Flyway 기초)
02-data-jpa         → Tier 2 - 7, 8 (JPA 심화 + Transaction)
03-security         → Tier 2 - 9    (Spring Security 7 + JWT/OAuth2)
04-testing          → Tier 2 - 10   (Testcontainers 본격)
05-observability    → Tier 2 - 11 + Tier 3 - 15 (Actuator + OTel)
06-modern-runtime   → Tier 3 - 12, 13 (Virtual Threads + Coroutines)
07-http-client      → Tier 3 - 14 (HTTP Interface)
08-native           → Tier 3 - 16 (GraalVM)
09-spring-ai        → Tier 3 - 17 (관심 있을 때)
```

각 모듈 네이밍은 루트 `README.md`의 `<2자리 번호>-<kebab-case 슬러그>` 컨벤션을 따른다.
