# Filter vs Interceptor

Spring MVC 요청이 흐를 때 횡단 관심사(cross-cutting concern)를 끼워 넣는 방식은 크게 두 가지: **Servlet Filter**와 **Spring HandlerInterceptor**. 둘 다 "요청 전/후에 끼어드는 훅"이라는 점에서 비슷해 보이지만, **계층이 다르고 알 수 있는 정보가 다르다**.

---

## 한눈 비교

| 항목 | Filter | Interceptor |
| --- | --- | --- |
| 정의된 곳 | Servlet 스펙 (`jakarta.servlet`) | Spring MVC (`org.springframework.web.servlet`) |
| 실행 위치 | DispatcherServlet **밖** | DispatcherServlet **안** |
| 핸들러 정보 | 모른다 (아직 HandlerMapping 전) | 안다 (`HandlerMethod`) |
| 요청/응답 객체 | `ServletRequest/Response` (래핑 가능) | `HttpServletRequest/Response` (그대로) |
| 등록 방식 | `FilterRegistrationBean` 또는 `@Component` | `WebMvcConfigurer#addInterceptors` |
| 매칭 단위 | URL 패턴 (`/*`, `/api/*`) | 핸들러 경로 패턴 (`/**`, `/api/**`) |
| 훅 시점 | `doFilter` 전/후 (단일) | `preHandle` / `postHandle` / `afterCompletion` (3단계) |
| 예외 처리 | `@ControllerAdvice` **밖** — 컨트롤러 예외도 여기 도달 | `@ControllerAdvice` **안** — 보통 변환된 응답만 본다 |
| 스프링 빈 주입 | 가능 (등록 시점에 Spring이 관리) | 항상 가능 |

---

## 흐름도

```
                    Client
                      │
                      ▼
   ┌─────────────────────────────────────────────────┐
   │ Servlet Container (Jetty 12)                    │
   │                                                 │
   │   ┌──────────────────────────────────┐          │
   │   │ Filter Chain                     │ ← 여기   │
   │   │  RequestLoggingFilter (before)   │          │
   │   │  CharacterEncodingFilter         │          │
   │   │  SecurityFilterChain (있다면)    │          │
   │   └────────────────┬─────────────────┘          │
   │                    ▼                            │
   │   ┌──────────────────────────────────┐          │
   │   │ DispatcherServlet                │          │
   │   │                                  │          │
   │   │   HandlerMapping (어디로 보낼지) │          │
   │   │            │                     │          │
   │   │            ▼                     │          │
   │   │   ┌──────────────────────┐       │          │
   │   │   │ Interceptor          │ ← 여기 (핸들러 알 수 있음)
   │   │   │  preHandle()         │       │          │
   │   │   └──────────┬───────────┘       │          │
   │   │              ▼                   │          │
   │   │   ┌──────────────────────┐       │          │
   │   │   │ @Controller 메서드   │       │          │
   │   │   └──────────┬───────────┘       │          │
   │   │              ▼                   │          │
   │   │   ┌──────────────────────┐       │          │
   │   │   │ Interceptor          │       │          │
   │   │   │  postHandle()        │       │          │
   │   │   └──────────┬───────────┘       │          │
   │   │              ▼                   │          │
   │   │   ┌──────────────────────┐       │          │
   │   │   │ View / Message       │       │          │
   │   │   │  Converter 렌더링    │       │          │
   │   │   └──────────┬───────────┘       │          │
   │   │              ▼                   │          │
   │   │   ┌──────────────────────┐       │          │
   │   │   │ Interceptor          │       │          │
   │   │   │  afterCompletion()   │ (예외 시에도 호출)
   │   │   └──────────────────────┘       │          │
   │   └────────────────┬─────────────────┘          │
   │                    ▲                            │
   │   Filter Chain (after) ── 응답 통과             │
   └─────────────────────────────────────────────────┘
                       │
                       ▼
                    Client
```

핵심:
- **Filter는 더 바깥** — DispatcherServlet 진입 전후를 감싼다. 어떤 컨트롤러가 처리할지 모른다.
- **Interceptor는 더 안쪽** — DispatcherServlet 내부에서 핸들러 호출 직전·직후에 끼어든다. `HandlerMethod`를 받아 어떤 컨트롤러 메서드인지 알 수 있다.
- 예외가 컨트롤러에서 던져지면: `@ControllerAdvice` → Interceptor의 `afterCompletion`(ex 인자 채워짐) → Filter의 `finally` 순으로 전파된다.

---

## 언제 무엇을 쓸까

### Filter가 자연스러운 경우

- **모든 요청에 무조건 적용되는 공통 처리**: 액세스 로그, 인코딩, GZip 압축, CORS.
- **요청·응답 본문 자체를 가공/캐싱**해야 할 때 — `ContentCachingRequestWrapper`로 본문을 두 번 읽거나 응답 본문을 가로채는 작업은 Filter 레벨에서 한다.
- **인증 같은 보안 결정** — Spring Security는 통째로 Filter Chain 위에 구현돼 있다. 컨트롤러에 도달하기 전에 차단해야 하므로.
- **DispatcherServlet 외부의 요청까지 포함**해야 할 때 — 정적 리소스, 다른 Servlet 등.

### Interceptor가 자연스러운 경우

- **컨트롤러 단위로 다르게 동작**해야 할 때 — 특정 메서드에 붙은 어노테이션 검사(`HandlerMethod.getMethodAnnotation(...)`), 핸들러 이름 기반 metric 태깅.
- **`ModelAndView`를 가공**하고 싶을 때 — 공통 모델 속성 주입 등.
- **핸들러 호출만의 소요 시간** 측정 (Filter는 View 렌더링까지 포함한 전체 시간).
- **Spring 빈을 자유롭게 주입**받아 도메인 로직과 엮어야 할 때 — Filter도 가능하지만 Interceptor가 더 자연스럽다.

### 둘 다 후보일 때의 결정 트리

```
Q1. 컨트롤러 매핑 정보(어떤 메서드인지)가 필요한가?
    ├─ YES → Interceptor
    └─ NO  → Q2
Q2. 요청/응답 본문을 가공·캐싱해야 하는가?
    ├─ YES → Filter (Wrapper로 감싸야 하므로)
    └─ NO  → Q3
Q3. 보안/인증처럼 컨트롤러 도달 전에 막아야 하는가?
    ├─ YES → Filter (Spring Security 연계)
    └─ NO  → 둘 다 가능. 더 안쪽(Interceptor)에 두는 게 일반적으로 단순.
```

---

## 이 모듈의 예제 코드

| 파일 | 역할 |
| --- | --- |
| [`web/filter/RequestLoggingFilter.kt`](../../src/main/kotlin/me/victor/spring01/web/filter/RequestLoggingFilter.kt) | 모든 요청의 method/URI/status/소요시간을 INFO로 로깅 |
| [`web/interceptor/HandlerTimingInterceptor.kt`](../../src/main/kotlin/me/victor/spring01/web/interceptor/HandlerTimingInterceptor.kt) | 핸들러 메서드 단위 소요 시간을 측정 (`HandlerMethod` 사용) |
| [`web/WebConfig.kt`](../../src/main/kotlin/me/victor/spring01/web/WebConfig.kt) | Filter는 `FilterRegistrationBean`, Interceptor는 `WebMvcConfigurer`로 등록 |
| [`web/PingController.kt`](../../src/main/kotlin/me/victor/spring01/web/PingController.kt) | 흐름 확인용 `GET /ping` |

### 실행해서 확인

```bash
./gradlew :01-mvc-basics:bootRun
# 다른 터미널에서
curl http://localhost:8080/ping
```

기대 로그 (순서 주목):

```
RequestLoggingFilter        : → GET /ping                       # Filter before
HandlerTimingInterceptor    : preHandle: PingController#ping    # Interceptor preHandle
HandlerTimingInterceptor    : handler PingController#ping ...   # Interceptor afterCompletion
RequestLoggingFilter        : ← GET /ping status=200 elapsed=…  # Filter after
```

Filter가 가장 바깥, Interceptor가 그 안쪽에 위치한다는 점이 로그 순서로 드러난다.

---

## 자주 빠지는 함정

1. **Interceptor에서 `afterCompletion` 대신 `postHandle`에 정리 로직을 쓴다** — 컨트롤러가 예외를 던지면 `postHandle`은 호출되지 않는다. 항상 호출돼야 하는 정리는 `afterCompletion`에.
2. **Filter에서 응답 본문을 읽으려 할 때 `ContentCachingResponseWrapper`를 잊는다** — 응답 스트림은 한 번만 쓰여진다. 캐싱 래퍼 없이는 body 로깅 불가.
3. **`@ControllerAdvice`로 처리한 예외는 Interceptor의 `ex` 인자에 안 잡힌다** — Spring이 이미 정상 응답으로 변환했기 때문. 원 예외를 봐야 한다면 Filter에서 잡거나, `HandlerExceptionResolver`를 사용해야 한다.
4. **인증을 Interceptor로 구현** — 동작은 하지만 권장되지 않는다. Spring Security의 Filter Chain과 책임이 겹치고 우선순위 꼬임의 원인이 된다.
