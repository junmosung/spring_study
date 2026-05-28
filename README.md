# spring-study

Spring 학습용 모노리포. 공통 인프라(PostgreSQL, Docker Compose, Gradle 래퍼)를 루트에서 공유하고, 번호 + 슬러그로 구분된 서브 프로젝트들이 각자의 Spring Boot 애플리케이션을 갖는다.

## 레이아웃

```
spring-study/
├── settings.gradle.kts        # 멀티 모듈 진입점
├── docker-compose.yml         # 공유 postgres
├── .env.example
├── gradle/, gradlew, gradlew.bat
├── docs/
│   └── getting-started.md     # 공통 실행 가이드 (env, compose, gradle 명령)
└── 01-mvc-basics/             # 첫 번째 학습 모듈
    ├── README.md
    ├── build.gradle.kts
    ├── docs/
    │   ├── 01-foundation/     # 환경·런타임·아키텍처
    │   ├── 02-ioc/            # IoC / DI / Bean / 외부 설정 바인딩
    │   └── 03-mvc/            # 요청 처리 / Filter / Interceptor / Validation
    └── src/
```

## 서브 프로젝트

| 번호 | 모듈 | 주제 |
| --- | --- | --- |
| 01 | [`01-mvc-basics/`](01-mvc-basics/) | Spring MVC 기본 구조 (Controller / Service / Repository / Domain 직접 구현) |

다음 모듈은 `02-…` 식으로 번호와 슬러그를 이어 붙여 추가한다. 추가 절차는 [`docs/getting-started.md`](docs/getting-started.md)의 "새 서브 프로젝트 추가하기" 섹션 참고.

학습 순서·핵심 개념·신규 기술 우선순위는 [`docs/learning-roadmap.md`](docs/learning-roadmap.md) 참고.

## Quick start

```bash
cp .env.example .env
docker compose up -d postgres
set -a; . ./.env; set +a
./gradlew :01-mvc-basics:bootRun
```

자세한 흐름과 트러블슈팅은 [`docs/getting-started.md`](docs/getting-started.md) 참고.

## 컨벤션

- 모듈 네이밍: `<2자리 번호>-<kebab-case 슬러그>` — 순서를 살리면서 의미를 같이 표현.
- 모듈별 문서는 `<module>/docs/`에, 공통 인프라 문서는 루트 `docs/`에.
- 공통 postgres를 사용하므로 모듈마다 다른 스키마/DB를 원할 때는 `application.yaml`의 `DB_NAME` 기본값을 모듈마다 다르게 지정한다.
