# `@ConfigurationProperties` + Kotlin data class

> Tier 1 #1 — IoC / DI / Bean 라이프사이클의 **세 번째 조각**.
> 등록·DI는 [`bean-registration-and-di.md`](./bean-registration-and-di.md), Scope·라이프사이클은 [`bean-scope-and-lifecycle.md`](./bean-scope-and-lifecycle.md).

설정 값(application.yaml, 환경변수, 프로파일)을 **타입 안전한 Kotlin 객체**로 묶어서 받는 표준 방법.

---

## 왜 `@Value` 대신 `@ConfigurationProperties` 인가

`@Value`는 한두 개 값 받을 때만:

```kotlin
@Service
class MailSender(
    @Value("\${mail.host}") private val host: String,
    @Value("\${mail.port}") private val port: Int,
    @Value("\${mail.from}") private val from: String,
)
```

- 키 이름을 문자열로 박아야 해서 오타가 컴파일 타임에 안 잡힌다.
- 같은 prefix의 값들이 사용처마다 흩어진다.
- 검증·기본값·중첩 구조·컬렉션을 표현하기 어렵다.
- 테스트 시 한 덩어리로 mock하기 까다롭다.

`@ConfigurationProperties`는 같은 prefix 아래의 값을 **타입 있는 객체 하나로 묶는다**:

```kotlin
@ConfigurationProperties(prefix = "mail")
data class MailProperties(val host: String, val port: Int, val from: String)
```

→ 사용처는 `MailProperties` 하나만 주입받고, 키 이름 타이핑 한 번도 필요 없음.

---

## Kotlin data class와의 궁합

```kotlin
@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val name: String,
    val cacheTtl: Duration = Duration.ofMinutes(5), // 기본값 가능
    val tags: List<String> = emptyList(),
)
```

- **Constructor binding 자동** — Spring Boot 3.0+ 부터 단일 생성자가 있으면 알아서 constructor binding 사용. **`@ConstructorBinding` 불필요**(deprecated).
- `val` → 불변. 런타임에 누가 값을 못 바꾼다.
- Kotlin 기본값 그대로 사용 → `@DefaultValue` 어노테이션 불필요.
- 중첩 data class·`List`·`Map`·`Duration`·`DataSize` 모두 OK.

---

## 등록 방법 3가지

| 방법 | 언제 |
| --- | --- |
| `@EnableConfigurationProperties(MyProps::class, ...)` on `@Configuration` | **명시적**. 어떤 properties가 활성되는지 한눈에 보임. 학습용·소규모에 적합. |
| `@ConfigurationPropertiesScan` on main class | 메인 클래스 하위 패키지 전체에서 `@ConfigurationProperties`를 자동 스캔. **모듈에 properties가 많을 때 가장 깔끔**. |
| `@Component` 를 properties 클래스에 추가 | 동작은 하지만 데이터/컴포넌트 시그니처가 섞여 컨벤션 어긋남. 비권장. |

### 결정 트리

```
Q. 모듈에 @ConfigurationProperties 클래스가 몇 개인가?
   ├─ 1~3개 → @EnableConfigurationProperties (명시적)
   └─ 많음   → @ConfigurationPropertiesScan (자동)
```

---

## 바인딩 규칙 (Relaxed Binding)

같은 키는 여러 형식으로 쓸 수 있다 — Spring이 모두 같은 필드로 매핑:

| application.yaml | 환경변수 | Kotlin 필드 |
| --- | --- | --- |
| `app.cache-ttl` | `APP_CACHE_TTL` | `cacheTtl` |
| `app.beta-ui` | `APP_BETA_UI` | `betaUi` |
| `app.owner.email` | `APP_OWNER_EMAIL` | `owner.email` |

### 특수 타입 자동 변환

| 타입 | 입력 예시 |
| --- | --- |
| `java.time.Duration` | `5s`, `30m`, `2h`, `PT1H30M` (ISO-8601도 OK) |
| `java.time.Period` | `1d`, `2w`, `3mo`, `P1Y2M3D` |
| `org.springframework.util.unit.DataSize` | `512KB`, `20MB`, `1GB` |
| `List<T>` | YAML 리스트 또는 콤마 구분 문자열 |
| `Map<K, V>` | YAML 맵 또는 `key1: v1, key2: v2` 문자열 |

---

## 다이어그램 — 바인딩 흐름

```
application.yaml / 환경변수 / --args / VM args
              │
              │  우선순위 (높은 → 낮은):
              │  1. command line --args
              │  2. SPRING_APPLICATION_JSON (env / property)
              │  3. OS 환경변수
              │  4. application-{profile}.yaml
              │  5. application.yaml
              │  6. @ConfigurationProperties data class 기본값
              ▼
         ┌─────────────────────────┐
         │  Spring Environment     │
         │  (key → value)          │
         └────────┬────────────────┘
                  │  Relaxed Binder
                  │  (kebab/camel/snake 매칭, 타입 변환)
                  ▼
         ┌─────────────────────────┐
         │  ConfigurationProperties│
         │  Bean (단일 인스턴스)   │
         └────────┬────────────────┘
                  │
                  │  @Validated가 있으면 jakarta 검증
                  ▼
              실패 → 기동 거부
              성공 → 컨테이너 등록
```

---

## 검증

```kotlin
@ConfigurationProperties(prefix = "mail")
@Validated                                    // ← 이거 없으면 어노테이션 무시됨
data class MailProperties(
    @NotBlank val host: String,
    @Min(1) @Max(65535) val port: Int,
    @Email val from: String,
)
```

검증 실패 시 **컨테이너 기동 자체가 거부**된다 (BindException). "잘못된 설정으로 띄우느니 빨리 죽자" 정책 — 운영 환경에서 큰 가치.

Kotlin data class에서 `@field:NotBlank` 같은 prefix가 보통은 필요하지만, 이 모듈의 `build.gradle.kts`에는 다음 컴파일러 옵션이 켜져 있어 그냥 `@NotBlank val host`로도 동작한다:

```kotlin
freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
```

→ val 파라미터의 어노테이션이 param + property 양쪽에 적용됨.

---

## 프로파일별 값

```
src/main/resources/
├── application.yaml          # 공통 / 기본값
├── application-dev.yaml      # SPRING_PROFILES_ACTIVE=dev 일 때 덮어쓰기
├── application-prod.yaml     # prod 일 때
└── application-test.yaml     # 테스트 컨텍스트
```

같은 키가 여러 파일에 있으면 **활성 프로파일의 값이 이긴다**. 그리고 환경변수 / 커맨드라인은 그것보다 더 위(위 다이어그램의 우선순위 표 참고).

---

## 이 모듈의 예제 코드

| 파일 | 역할 |
| --- | --- |
| [`learning/config/AppProperties.kt`](../../src/main/kotlin/me/victor/spring01/learning/config/AppProperties.kt) | 중첩 / 컬렉션 / Map / Duration / DataSize / 기본값 |
| [`learning/config/MailProperties.kt`](../../src/main/kotlin/me/victor/spring01/learning/config/MailProperties.kt) | `@Validated` + jakarta validation |
| [`learning/config/ConfigDemoConfig.kt`](../../src/main/kotlin/me/victor/spring01/learning/config/ConfigDemoConfig.kt) | `@EnableConfigurationProperties` + 시작 시 출력 |
| [`src/main/resources/application.yaml`](../../src/main/resources/application.yaml) | `app.*` / `mail.*` 섹션 |

### 실행해서 확인

```bash
./gradlew :01-mvc-basics:bootRun
```

기동 로그 후반부:

```
ConfigDemoRunner : app  = AppProperties(name=spring-study, owner=Owner(name=Victor, email=victor@example.com), features=Features(signup=true, betaUi=false), tags=[learning, kotlin], timeouts={http=PT5S, db=PT2S}, cacheTtl=PT10M, maxUploadSize=20MB)
ConfigDemoRunner : mail = MailProperties(host=smtp.example.com, port=587, from=noreply@example.com)
```

### 검증 실패 확인 (선택)

`application.yaml`에서 `mail.from`을 `not-an-email`로 바꾸면 기동 시 다음과 비슷한 오류:

```
Binding to target ... failed:
    Property: mail.from
    Value: "not-an-email"
    Reason: 올바른 형식의 이메일 주소여야 합니다.
```

---

## 자주 빠지는 함정

1. **`@Value`를 많이 쓴다** — 한두 개면 OK, 그 이상은 `@ConfigurationProperties`로 묶는다.
2. **`@Validated` 빼먹기** — 어노테이션은 있는데 검증이 안 도는 가장 흔한 원인.
3. **여전히 `@ConstructorBinding` 붙임** — Boot 3.0+ deprecated. 단일 생성자면 자동.
4. **`var` 사용** — 불변성 잃고, 옛 setter binding 경로로 떨어져 디버깅 어려워짐. `val` 고수.
5. **등록을 잊는다** — `@EnableConfigurationProperties` 또는 `@ConfigurationPropertiesScan` 없으면 컨테이너에 안 올라옴. `NoSuchBeanDefinitionException`.
6. **민감 값(비밀번호, 토큰)을 yaml에 직접 박는다** — `.env`/환경변수/Secret Manager로 분리. Relaxed binding 덕분에 환경변수로 그대로 받힌다.
7. **`Duration`을 `Long` 밀리초로 받는다** — 코드에서 단위 환산이 흩어진다. `Duration` 타입으로 받고 YAML에선 `30s`, `5m` 같이 가독성 있게.

---

## 다음

- Tier 1 #2 — Spring MVC 요청 흐름 (DispatcherServlet, HandlerMapping, MessageConverter)
- Tier 1 #3 — Validation + `ProblemDetail`

여기까지 오면 Tier 1 #1(IoC / DI / Bean 라이프사이클) 학습은 완성.
