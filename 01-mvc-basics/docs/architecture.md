# 01-mvc-basics 아키텍처 개요

Spring MVC 학습용 첫 번째 서브 프로젝트의 환경 구성과 전반적인 흐름을 정리한 문서.
MVC 계층(Controller / Service / Repository / Domain)은 학습 목적상 직접 구현하므로 본 문서에는 포함하지 않는다.
모노리포 전반 / 공유 인프라(postgres, env)에 대한 내용은 루트의 [`docs/getting-started.md`](../../docs/getting-started.md) 참고.

## 기술 스택

| 영역 | 사용 기술 |
| --- | --- |
| 언어 | Kotlin 2.2.21 / JDK 24 |
| 프레임워크 | Spring Boot 4.0.6 (Spring Framework 7) |
| 빌드 | Gradle (Kotlin DSL) |
| 영속성 | Spring Data JPA (Hibernate) |
| DB | PostgreSQL 17 |
| 마이그레이션 | Flyway |
| 컨테이너 | Docker / Docker Compose |
| 검증 | Jakarta Bean Validation |
| JSON | Jackson + jackson-module-kotlin |

## 디렉토리 구조 (모노리포 관점)

```
spring-study/                   # 루트 (Git 리포)
├── settings.gradle.kts         # rootProject.name="spring-study", include("01-mvc-basics")
├── docker-compose.yml          # 공유 인프라(postgres)
├── .env.example
├── gradle/, gradlew, gradlew.bat   # 공유 Gradle 래퍼
├── docs/
│   └── getting-started.md      # 공통 실행 가이드
├── README.md
└── 01-mvc-basics/              # 본 서브 프로젝트
    ├── build.gradle.kts        # 이 모듈의 의존성/플러그인
    ├── Dockerfile              # 본 모듈 빌드용 (필요 시)
    ├── .dockerignore
    ├── docs/
    │   ├── architecture.md     # 본 문서
    │   ├── setup-verification.md
    │   └── jetty-vs-tomcat.md
    └── src/
        ├── main/
        │   ├── kotlin/me/victor/spring01/
        │   │   └── Spring01Application.kt
        │   └── resources/
        │       ├── application.yaml
        │       └── db/migration/   # Flyway SQL 위치
        └── test/
            └── kotlin/me/victor/spring01/
                └── Spring01ApplicationTests.kt
```

MVC 계층을 추가할 때 권장 패키지 구조 (예시):

```
me.victor.spring01
├── Spring01Application.kt
├── config/        # @Configuration 클래스
├── domain/        # 엔티티, 값 객체 (도메인별 하위 패키지 권장)
├── repository/    # JpaRepository 인터페이스
├── service/       # 비즈니스 로직
├── controller/    # @RestController / @Controller
└── dto/           # 요청·응답 DTO (도메인 패키지 하위에 둬도 무방)
```

## 설정 흐름

1. `application.yaml`은 모든 DB 관련 값을 환경변수 `${DB_*}`로 주입받는다.
2. 로컬 실행 시: 셸 환경변수 또는 IDE Run Configuration 환경변수로 주입.
3. Docker Compose 실행 시: 프로젝트 루트의 `.env` 파일을 Compose가 자동으로 읽어 `docker-compose.yml`의 `${VAR}`를 치환하고, `app` 서비스의 `environment:`로 전달.
4. 앱 컨테이너에서는 `DB_HOST=postgres`로 오버라이드되어 같은 네트워크의 `postgres` 서비스를 가리킨다.

## 애플리케이션 부팅 순서

```
Spring01Application.main()
        │
        ▼
@SpringBootApplication 자동 구성 시작
        │
        ▼
DataSource(HikariCP) 초기화  ── jdbc:postgresql://...
        │
        ▼
Flyway 실행 (spring.flyway.enabled=true)
  └─ classpath:db/migration/V*.sql 순서대로 적용
  └─ 적용 이력은 flyway_schema_history 테이블에 기록
        │
        ▼
Hibernate 초기화 (ddl-auto: validate)
  └─ 엔티티 매핑이 실제 스키마와 일치하지 않으면 부팅 실패
        │
        ▼
DispatcherServlet 등록 + 8080 포트 리스닝
```

`ddl-auto`를 `validate`로 둔 이유는 학습 목적상 **스키마 권한은 Flyway가, 매핑 검증은 Hibernate가** 담당하도록 책임을 분리하기 위해서다. 엔티티만 만들고 DB는 자동 생성되지 않으니 반드시 Flyway 마이그레이션을 함께 작성해야 한다.

## Spring Boot 4.0 자동구성 메모

Spring Boot 4.0부터 자동구성이 기능별 모듈로 분리됐다(`spring-boot-jdbc`, `spring-boot-data-jpa`, `spring-boot-flyway` …).
`spring-boot-starter-data-jpa`만 추가해도 JDBC/JPA 자동구성은 따라오지만 **Flyway는 따라오지 않는다**.
이 때문에 `build.gradle.kts`에는 `flyway-core` / `flyway-database-postgresql` 외에 `org.springframework.boot:spring-boot-flyway`를 명시적으로 추가했다. 빠뜨리면 부팅은 성공하지만 마이그레이션이 조용히 건너뛰어진다(자동구성이 적용되지 않아 Flyway 빈 자체가 생성되지 않음).

## Flyway 마이그레이션 규칙

- 위치: `src/main/resources/db/migration/`
- 네이밍: `V{version}__{description}.sql` (예: `V1__create_users.sql`)
- 적용 후 파일을 수정하면 체크섬이 변경되어 부팅이 실패한다. 새 변경은 항상 새 버전 파일로 추가한다.
- 첫 마이그레이션부터 V1로 시작하면 된다. `baseline-on-migrate=true`는 기존 DB가 있는 상황을 대비한 옵션.

## 요청 처리 흐름 (MVC 구현 후 일반 형태)

```
HTTP Request
   │
   ▼
DispatcherServlet
   │
   ▼
@RestController / @Controller    ← controller 패키지
   │  (DTO 매핑 + @Valid 검증)
   ▼
Service                          ← service 패키지 (@Transactional)
   │
   ▼
JpaRepository                    ← repository 패키지
   │
   ▼
Hibernate → PostgreSQL
```

## Kotlin + JPA 주의사항

- `kotlin("plugin.jpa")`가 `@Entity`, `@MappedSuperclass`, `@Embeddable` 클래스에 no-arg 생성자를 합성한다.
- `allOpen` 블록이 동일 어노테이션이 붙은 클래스를 자동으로 `open`으로 만들어 Hibernate 프록시가 동작한다.
- `data class`로 엔티티를 만드는 것은 비권장: `equals/hashCode/copy`가 모든 프로퍼티 기반으로 생성되어 양방향 연관관계나 지연 로딩과 충돌이 잦다. 일반 `class`에 필요한 메서드만 직접 정의하는 것을 권장.

## 테스트 전략 가이드 (향후 작성 시 참고)

- 단위 테스트: 순수 Kotlin/JUnit 5 — 서비스 로직 검증.
- 슬라이스 테스트: `@DataJpaTest` + Testcontainers PostgreSQL — 리포지토리 검증.
- 통합 테스트: `@SpringBootTest` + Testcontainers — 컨트롤러부터 DB까지 전체 흐름.
- Flyway는 테스트 컨텍스트에서도 동일하게 실행되므로 마이그레이션이 곧 테스트 스키마가 된다.

실행 방법은 [`getting-started.md`](./getting-started.md) 참고.
