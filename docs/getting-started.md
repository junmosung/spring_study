# 실행 가이드 (모노리포 공통)

`spring-study` 모노리포는 공유 인프라(postgres) 한 벌과 번호 + 슬러그로 구분된 서브 프로젝트들(`01-mvc-basics`, `02-…` …)로 구성된다.
서브 프로젝트별 세부 내용은 각 모듈의 `docs/`에 둔다.

## 0. 사전 준비

- JDK 24 (로컬 실행 시)
- Docker Desktop (또는 Docker Engine + Compose v2)

## 1. 환경변수 설정

`.env.example`을 복사해 `.env`로 만든다. 학습 단계에선 기본값 그대로 사용해도 무방.

```bash
cp .env.example .env
```

| 변수 | 설명 | 기본값 |
| --- | --- | --- |
| `DB_HOST` | 로컬 실행 시 DB 호스트 (compose 내부에선 자동으로 `postgres`) | `localhost` |
| `DB_PORT` | 호스트에 노출되는 PostgreSQL 포트 | `5432` |
| `DB_NAME` | 데이터베이스 이름 | `spring01` |
| `DB_USERNAME` | DB 사용자 | `spring01` |
| `DB_PASSWORD` | DB 비밀번호 | `spring01` |
| `SERVER_PORT` | 앱이 노출할 포트 | `8080` |

> 호스트의 5432 포트가 이미 점유돼 있다면 `.env`에서 `DB_PORT`를 다른 값(예: `5433`)으로 변경.

## 2. 공유 인프라 띄우기

```bash
docker compose up -d postgres
docker compose ps                 # healthy 상태 확인
docker compose logs postgres -f   # 필요 시 로그
```

postgres는 한 번만 띄우면 모든 서브 프로젝트가 같은 DB를 공유한다. 프로젝트별로 스키마/데이터를 분리하고 싶다면 각 모듈에서 `DB_NAME`을 다르게 주입한다.

종료:

```bash
docker compose down              # 컨테이너만 제거 (볼륨 유지)
docker compose down -v           # 볼륨까지 제거 (DB 데이터 초기화)
```

## 3. 서브 프로젝트 실행

루트에서 Gradle 멀티 모듈 경로를 지정해 실행한다.

```bash
# 환경변수 셸에 주입
set -a; . ./.env; set +a

# 01-mvc-basics 부팅
./gradlew :01-mvc-basics:bootRun

# 01-mvc-basics 테스트
./gradlew :01-mvc-basics:test

# 전체 빌드 (모든 모듈)
./gradlew build
```

IntelliJ에서는 Gradle 패널에서 `spring-study > 01-mvc-basics > Tasks > application > bootRun`을 더블 클릭하거나, Run Configuration의 Working directory를 루트로 두고 Tasks에 `:01-mvc-basics:bootRun`을 지정한다. 환경변수는 EnvFile 플러그인으로 `.env`를 로딩하거나 Run Configuration에 직접 입력.

## 4. 새 서브 프로젝트 추가하기

1. 루트에 디렉토리 생성: `02-jpa-relations/` 같은 식으로 번호+슬러그.
2. `02-jpa-relations/build.gradle.kts` 작성 (`01-mvc-basics`를 복사해 시작해도 무방).
3. `src/main/kotlin/...`, `src/main/resources/...` 구조 추가.
4. 루트 `settings.gradle.kts`에 한 줄 추가:
   ```kotlin
   include("02-jpa-relations")
   ```
5. 필요하면 `02-jpa-relations/docs/`에 모듈별 문서 작성.

`DB_NAME`을 모듈마다 분리하고 싶다면 모듈의 `application.yaml`에서 다른 기본값을 쓰거나, 실행 시 환경변수를 오버라이드.

## 5. Flyway 마이그레이션

서브 프로젝트별로 자체 클래스패스를 가지므로 마이그레이션도 모듈 안에서 관리한다.

- 위치: `<module>/src/main/resources/db/migration/`
- 네이밍: `V{version}__{description}.sql`
- 이미 적용된 V 파일을 수정하면 체크섬 오류 → 새 버전 파일로 추가.

## 6. DB 직접 접속 (디버깅용)

```bash
docker exec -it spring-study-postgres psql -U "$DB_USERNAME" -d "$DB_NAME"
```

## 7. 자주 발생하는 문제

| 증상 | 원인 / 해결 |
| --- | --- |
| `Task 'bootRun' not found` | 루트에서 호출 시 모듈 경로 누락. `./gradlew :01-mvc-basics:bootRun` 형태로. |
| `Connection refused` | postgres가 아직 healthy 아님. `docker compose ps`로 상태 확인. |
| `port is already allocated` | 호스트 5432가 다른 컨테이너/프로세스가 점유. `.env`의 `DB_PORT` 변경. |
| Flyway `checksum mismatch` | 적용된 V 파일을 수정함. 새 버전으로 분리하거나, dev 환경에서 `docker compose down -v` 후 재기동. |
| Hibernate `Schema-validation: missing table` | 엔티티만 만들고 마이그레이션을 안 작성. 해당 모듈의 `db/migration`에 SQL 추가. |
