# 환경 구성 검증 기록

`docker compose up -d postgres` + `./gradlew :01-mvc-basics:bootRun`으로 앱이 PostgreSQL에 정상 연결되는지 확인한 결과 정리.
이후 동일 문제 발생 시 참고용.

## 1. 연결 확인 결과

| 항목 | 값 |
| --- | --- |
| JDBC URL | `jdbc:postgresql://localhost:5433/spring01` |
| DB 버전 | PostgreSQL 17.10 (`postgres:17-alpine`) |
| 기본 스키마 | `spring01/public` |
| 커넥션 풀 | HikariCP — `HikariPool-1 Added connection` 확인 |
| Hibernate | ORM 7.2.12.Final, `PostgreSQLDialect` |
| 웹 서버 | Tomcat 11.0.21, 포트 8080 |
| 부팅 시간 | ≈ 1.6s (`Started Spring01ApplicationKt in 1.596 seconds`) |

> 호스트 5432 포트가 다른 컨테이너(`chat-postgres`)에 점유돼 있어 `.env`의 `DB_PORT`를 `5433`으로 변경. 컨테이너 내부 포트는 그대로 5432.

## 2. 발견한 이슈와 수정 — Spring Boot 4.0 Flyway 자동구성 누락

### 증상
- 앱은 정상 부팅하고 DB 연결도 성공
- 그러나 Flyway 관련 로그가 전혀 출력되지 않음
- `flyway_schema_history` 테이블도 생성되지 않음
- `src/main/resources/db/migration/`에 마이그레이션 파일을 둬도 적용되지 않음 (조용히 건너뜀)

### 원인
Spring Boot 4.0부터 자동구성이 기능별 모듈로 쪼개졌다.

```
spring-boot-autoconfigure   ← 코어 자동구성만
spring-boot-jdbc            ← JDBC 자동구성 (DataSource 등)
spring-boot-data-jpa        ← JPA 자동구성
spring-boot-flyway          ← Flyway 자동구성  ← 별도 모듈!
spring-boot-tomcat          ← Tomcat 자동구성
...
```

`spring-boot-starter-data-jpa`는 `spring-boot-jdbc` / `spring-boot-data-jpa`까지만 끌어오고 **`spring-boot-flyway`는 끌어오지 않는다**. `flyway-core`만 의존성으로 추가하면 클래스패스에는 있지만 자동구성이 트리거되지 않아 `Flyway` 빈이 생성되지 않는다.

### 해결
`build.gradle.kts`에 자동구성 모듈을 명시적으로 추가.

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-flyway")   // ← 추가
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
}
```

추가 후 재기동 시 로그:

```
o.f.core.internal.command.DbMigrate : Migrating schema "public" to version "1 - smoke test"
o.f.core.internal.command.DbMigrate : Successfully applied 1 migration to schema "public", now at version v1
```

`flyway_schema_history` 테이블도 정상 생성됨.

### 같은 실수를 피하는 방법
Spring Boot 4.0 환경에서 새 라이브러리를 붙일 때는 `spring-boot-<name>` 형태의 자동구성 모듈이 별도로 존재하는지 확인하고 명시적으로 추가한다. 의심될 땐 아래 명령으로 클래스패스 점검.

```bash
./gradlew -q dependencies --configuration runtimeClasspath | grep spring-boot-
```

## 3. 환경 정리 상태

검증 후 테스트용 파일과 데이터를 모두 제거. 사용자가 첫 마이그레이션을 V1부터 자유롭게 작성할 수 있는 상태.

| 정리 항목 | 상태 |
| --- | --- |
| `V1__smoke_test.sql` | 삭제 |
| `smoke_test` 테이블 | DROP |
| `flyway_schema_history` 테이블 | DROP (사용자 V1 적용 시 다시 생성됨) |
| postgres 컨테이너 | 실행 중 (포트 5433 매핑) |

## 4. 재현 절차

```bash
cp .env.example .env                       # 필요 시 DB_PORT 충돌 회피
docker compose up -d postgres              # postgres만 띄움
set -a; . ./.env; set +a                   # 셸에 .env 주입
./gradlew :01-mvc-basics:bootRun           # 앱 부팅
# 로그에서 "Migrating schema" 와 "Started Spring01ApplicationKt" 확인
```

DB 직접 확인:

```bash
docker exec spring-study-postgres psql -U spring01 -d spring01 -c "\dt"
docker exec spring-study-postgres psql -U spring01 -d spring01 \
    -c "SELECT version, description, success, installed_on FROM flyway_schema_history;"
```
