# spring_study

Spring MVC 학습용 프로젝트. Kotlin + Spring Boot 4.0 + PostgreSQL + Flyway + Docker Compose.

## Docs

- [`docs/architecture.md`](docs/architecture.md) — 스택, 디렉토리 구조, 부팅/요청 흐름
- [`docs/getting-started.md`](docs/getting-started.md) — 환경변수, 실행 방법, 트러블슈팅
- [`docs/setup-verification.md`](docs/setup-verification.md) — 초기 연결 검증 기록, Spring Boot 4 Flyway 자동구성 메모
- [`docs/jetty-vs-tomcat.md`](docs/jetty-vs-tomcat.md) — 임베디드 컨테이너를 Jetty로 교체한 이유와 비교

## Quick start

```bash
cp .env.example .env
docker compose up -d postgres
set -a; . ./.env; set +a
./gradlew bootRun
```
