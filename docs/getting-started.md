# 실행 가이드

## 0. 사전 준비

- JDK 24 (로컬 실행 시)
- Docker Desktop (또는 Docker Engine + Compose v2)

## 1. 환경변수 설정

`.env.example`을 복사해 `.env`로 만든다. 운영 비밀이 아니므로 학습 단계에선 기본값을 그대로 써도 무방하다.

```bash
cp .env.example .env
```

| 변수 | 설명 | 기본값 |
| --- | --- | --- |
| `DB_HOST` | 로컬 실행 시 DB 호스트 (Compose에선 무시되고 `postgres`로 덮어씀) | `localhost` |
| `DB_PORT` | 호스트에 노출되는 PostgreSQL 포트 | `5432` |
| `DB_NAME` | 데이터베이스 이름 | `spring01` |
| `DB_USERNAME` | DB 사용자 | `spring01` |
| `DB_PASSWORD` | DB 비밀번호 | `spring01` |
| `SERVER_PORT` | 앱이 노출할 포트 | `8080` |

## 2. 실행 시나리오

### 시나리오 A — DB만 Docker로 띄우고 앱은 IDE/Gradle로 실행

학습 중 가장 자주 쓰게 될 흐름.

```bash
docker compose up -d postgres
./gradlew bootRun
```

IntelliJ에서 실행할 때는 Run Configuration → Environment variables에 `.env` 값을 동일하게 넣거나 EnvFile 플러그인을 사용한다.

종료:

```bash
docker compose down              # 컨테이너만 제거
docker compose down -v           # 볼륨까지 제거 (DB 데이터 초기화)
```

### 시나리오 B — 앱과 DB 모두 Docker Compose로 빌드/실행

배포 가까운 형태로 확인할 때.

```bash
docker compose up --build
```

백그라운드로 띄우려면 `-d` 추가. 로그 확인은 `docker compose logs -f app`.

## 3. 테스트 실행

```bash
./gradlew test
```

> 주의: 현재 `application.yaml`이 PostgreSQL을 가리키므로 테스트 컨텍스트가 뜨려면 DB가 떠 있어야 한다. 통합 테스트가 추가될 때 Testcontainers를 도입하면 외부 DB 의존을 제거할 수 있다.

## 4. Flyway 마이그레이션 추가하기

1. `src/main/resources/db/migration/` 아래에 `V1__create_xxx.sql` 형식으로 파일 추가.
2. 앱을 재기동하면 미적용 마이그레이션이 자동 실행된다.
3. 적용 이력은 `flyway_schema_history` 테이블에서 확인 가능.

이미 적용된 V 파일을 수정하면 체크섬 오류가 발생한다. 변경이 필요하면 항상 다음 버전 파일을 추가한다.

## 5. DB 직접 접속 (디버깅용)

```bash
docker exec -it spring01-postgres psql -U "$DB_USERNAME" -d "$DB_NAME"
```

## 6. 자주 발생하는 문제

| 증상 | 원인 / 해결 |
| --- | --- |
| 앱 부팅 시 `Schema-validation: missing table` | 엔티티는 만들었지만 Flyway 마이그레이션이 없음. SQL 파일 추가 후 재기동. |
| Flyway `Validate failed: Migration checksum mismatch` | 이미 적용된 V 파일을 수정함. 새 버전으로 분리하거나 dev 환경 한정으로 볼륨 삭제 후 재기동. |
| `Connection refused` | postgres 컨테이너가 아직 healthy 상태가 아님. `docker compose ps`로 상태 확인. |
| 포트 충돌 | `.env`의 `DB_PORT` 또는 `SERVER_PORT`를 다른 값으로 변경. |
