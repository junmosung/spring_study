# Spring 생태계 패턴 카탈로그

> Spring/Spring Boot에서 *실제로 만나는* 패턴들을 한 자리에 모은 레퍼런스.
> 관련 문서:
> - [`solid-and-hexagonal.md`](./solid-and-hexagonal.md) — SOLID/Hexagonal 비판적 정리
> - [`solid-and-hexagonal-deep-dive.md`](./solid-and-hexagonal-deep-dive.md) — SOLID/Hexagonal 심층 이해

크게 4축으로 정리합니다.

1. **GoF 패턴** — Spring 내부에서 직접 활용
2. **PoEAA / 엔터프라이즈 도메인 패턴** — Fowler 계열
3. **아키텍처 스타일** — 시스템 전체의 모양
4. **한국 Spring 커뮤니티 최근 유행** — 2023~2026

---

## 1. GoF 패턴 — Spring 내부에서 직접 활용되는 것들

| 패턴 | Spring에서의 발현 | 학습 시 보면 좋은 클래스 |
|---|---|---|
| **Singleton** | `@Component`/`@Bean`의 기본 스코프 | `DefaultListableBeanFactory` |
| **Prototype** | `@Scope("prototype")` | — |
| **Factory Method** | `@Bean` 메서드, `BeanFactory.getBean()` | `BeanFactory` |
| **Abstract Factory** | `FactoryBean<T>` | `FactoryBean`, `SqlSessionFactoryBean` |
| **Builder** | `MockMvcBuilders`, `UriComponentsBuilder`, Security DSL | — |
| **Prototype(객체 복제)** | `BeanUtils.copyProperties` | — |
| **Adapter** | `HandlerAdapter`(`RequestMappingHandlerAdapter` 등) | `HandlerAdapter` |
| **Bridge** | `JdbcTemplate` — 추상 API와 드라이버 분리 | — |
| **Composite** | Filter Chain, `CompositeHealthContributor` | — |
| **Decorator** | `BeanPostProcessor`, `HandlerInterceptor` 체인, `TransactionAwareDataSourceProxy` | — |
| **Facade** | `JdbcTemplate`, `RestClient` — 복잡한 인프라를 단순 API로 | — |
| **Proxy** | **AOP 핵심**. `@Transactional`/`@Async`/`@Cacheable`은 모두 동적 프록시 | `ProxyFactory`, `JdkDynamicAopProxy`, `CglibAopProxy` |
| **Chain of Responsibility** | `FilterChain`, `HandlerInterceptor`, Spring Security `SecurityFilterChain` | — |
| **Command** | `@RequestMapping` 핸들러, `Job`/`Step`(Spring Batch) | — |
| **Mediator** | `ApplicationEventPublisher` | — |
| **Observer** | `ApplicationListener`, `@EventListener` | — |
| **State** | Spring Statemachine | — |
| **Strategy** | `PasswordEncoder`, `ViewResolver`, `HttpMessageConverter`, `AuthenticationProvider` | — |
| **Template Method** | `JdbcTemplate`, `RestTemplate`, `JmsTemplate`, `TransactionTemplate` | `JdbcTemplate.execute` |
| **Visitor** | `BeanDefinitionVisitor`(드물게) | — |

> **포인트**: Spring 학습은 사실상 GoF 패턴 학습과 거의 동의어. 프레임워크가 패턴 카탈로그처럼 설계됨.

---

## 2. 엔터프라이즈 / 도메인 패턴 — Martin Fowler PoEAA 계열

*Patterns of Enterprise Application Architecture* (2002)에서 정리된 것들로, Spring/Spring Data가 다수 직접 구현.

| 패턴 | 의미 | Spring 매핑 |
|---|---|---|
| **Repository** | 도메인 객체 컬렉션 추상화 | `JpaRepository`, `CrudRepository` |
| **Specification** | 쿼리 조건을 객체로 캡슐화 | `org.springframework.data.jpa.domain.Specification` |
| **Unit of Work** | 변경 추적 후 한 번에 flush | JPA의 영속성 컨텍스트(`EntityManager`) |
| **Data Mapper** | 도메인과 DB 매핑 분리 | Hibernate, MyBatis |
| **Active Record** | 도메인 객체가 영속화 메서드를 직접 가짐 | (Spring은 권장 안 함; Ruby on Rails 스타일) |
| **DTO** | 레이어 간 데이터 전달용 객체 | `@RequestBody`/`@ResponseBody` 대상 |
| **Service Layer** | 트랜잭션·유스케이스 경계 | `@Service`(관례) |
| **Front Controller** | 모든 요청을 단일 진입점으로 라우팅 | `DispatcherServlet` |
| **Page Controller** | 페이지별 컨트롤러 | `@Controller`의 핸들러 메서드 |
| **Template View** | 템플릿에 데이터 바인딩 | Thymeleaf, Mustache |
| **Identity Map** | 같은 ID의 객체는 메모리에 단 하나 | JPA 1차 캐시 |
| **Lazy Load** | 필요할 때 로드 | `FetchType.LAZY`, `@LazyCollection` |
| **Optimistic Offline Lock** | 버전 필드로 충돌 감지 | `@Version` |
| **Pessimistic Offline Lock** | DB lock으로 차단 | `@Lock(LockModeType.PESSIMISTIC_WRITE)` |

---

## 3. 아키텍처 스타일 — 시스템 전체의 모양

| 스타일 | 핵심 | Spring 도구 |
|---|---|---|
| **Layered (전통적)** | Controller → Service → Repository → DB | 별다른 도구 없음, 컨벤션 |
| **Hexagonal / Ports & Adapters** | 도메인 보호, 어댑터 교체 가능 | (라이브러리 없음, 폴더 컨벤션) |
| **Onion / Clean** | 동심원, Dependency Rule | (라이브러리 없음) |
| **Modular Monolith** | 한 프로세스 안에서 모듈 경계를 코드로 강제 | **Spring Modulith** |
| **Microservices** | 서비스별 독립 배포 | Spring Cloud 생태계 |
| **Event-Driven** | 컴포넌트 간 비동기 메시지 | `ApplicationEvent`, Spring for Apache Kafka, Spring Cloud Stream |
| **CQRS** | Command/Query 모델 분리 | Axon Framework, 수제 구현 |
| **Event Sourcing** | 상태 대신 이벤트의 적분으로 시스템 상태 표현 | Axon, EventStoreDB |
| **Saga** | 분산 트랜잭션을 보상 트랜잭션 시퀀스로 대체 | Axon, 수제 구현 |
| **Outbox Pattern** | DB 트랜잭션과 메시지 발행의 원자성 보장 | 수제 구현, Debezium |
| **Reactive Pipeline** | backpressure 기반 비동기 스트림 | Spring WebFlux, Reactor, R2DBC |
| **Pipes and Filters** | 입력 → 일련의 필터 → 출력 | Spring Batch, Spring Integration |

---

## 4. 한국 Spring 커뮤니티의 최근 유행 (2023~2026)

| 유행 | 주된 출처 | 현실적 평가 |
|---|---|---|
| **DDD(전술 패턴)** | Eric Evans 2003, 우아한형제들·토스 발표 | Entity/VO/Aggregate/Repository는 거의 표준어가 됨. Bounded Context는 모놀리스에서도 가치 있음 |
| **헥사고날 + DDD** | 토스 ID, 우아한형제들 결제 등 | CRUD 위주 서비스엔 과함. 복잡 도메인엔 정수 |
| **클린 아키텍처** | Uncle Bob 책 + 한국어 번역서 인기 | 헥사고날과 거의 동의어로 쓰임. 폴더 컨벤션만 차용한 경우가 많음 |
| **모듈러 모놀리스** | Spring Modulith(2022~), DDD Europe 발표 | MSA 피로감의 반작용. 실용적 |
| **CQRS** | DDD 진영 | 90%의 서비스에선 과함. Read 쪽만 뷰 모델 분리하는 *경량 CQRS*가 현실적 |
| **이벤트 소싱** | Axon, EventStoreDB | 정말 필요한 곳은 극히 드묾. *유행으로 도입했다가 후회*하는 대표 사례 |
| **Outbox Pattern** | 마이크로서비스 일관성 논의 | 메시지를 쓰는 시스템이면 거의 필수 패턴 |
| **Saga** | MSA + 분산 트랜잭션 회피 | MSA 한정. 모놀리스에선 그냥 `@Transactional` |
| **함수형 도메인 모델링** | Scott Wlaschin *Domain Modeling Made Functional* | Kotlin의 sealed class/data class와 궁합 좋음. 작은 도메인에 적합 |
| **TDD/리팩터링 + 헥사고날** | Tom Hombergs 책 | 가장 건전한 조합. *테스트를 위해* 아키텍처가 따라오는 자연스러운 동기 |

### 유행 진단 체크리스트

도입을 검토할 때 자문할 질문:

1. **이 패턴이 해결하는 *고통*을 우리가 실제로 겪고 있는가?** — No면 도입하지 마라.
2. **도입 비용(학습/보일러플레이트/팀 합의)을 회수할 수명의 시스템인가?** — 6개월 안에 폐기될 거면 No.
3. **블로그/컨퍼런스 발표자의 *조직 규모*가 우리와 비슷한가?** — 토스/우아한형제들 사례를 5인 스타트업이 그대로 가져오면 거의 실패.
4. **걸음마 단계라면, *경량 버전*만 도입해도 가치를 얻을 수 있는가?** — DDD 전체 대신 Aggregate 경계만, 헥사고날 전체 대신 Repository 추상화만.

---

## 학습 로드맵과의 연결

- **Tier 1~2 (`01-mvc-basics`, `02-data-jpa`)**: GoF + PoEAA 패턴이 Spring 학습 자체에 녹아 있음 → 따로 공부 불필요, **발견**하면 됨.
- **Tier 3 #21 Spring Modulith**: 모듈러 모놀리스 진입점.
- **Tier 5 #26 AOP 내부**: Proxy 패턴 깊이 학습. JDK Dynamic Proxy vs CGLIB.

---

## 종합 결론

- **GoF**는 프레임워크 *내부*에 깊이 박혀 있어 Spring 학습 자체가 패턴 학습.
- **PoEAA**의 데이터 액세스 패턴은 Spring Data가 거의 표준화.
- **아키텍처 스타일**은 *팀 합의*의 결과지 Spring이 강제하지 않음 — 자유롭게 선택 가능.
- **한국 커뮤니티 유행**은 DDD·헥사고날·모듈러 모놀리스가 주축. *어떤 고통에서 나왔는지*를 알고 도입해야 카고 컬트를 면함.