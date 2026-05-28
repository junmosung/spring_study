# Spring MVC 요청 흐름

> Tier 1 #2 — `DispatcherServlet` 안에서 일어나는 일을 분해해서 본다.
> 그 **바깥쪽**(Servlet Filter 영역)과 **DispatcherServlet 내부의 핸들러 훅**은 [`filter-vs-interceptor.md`](./filter-vs-interceptor.md)에서 별도로 다룬다.

HTTP 요청 한 건이 들어와 응답이 나갈 때까지, Spring MVC가 하는 일을 단계별로 추적한다. 흐름을 한 번 머릿속에 그려두면 "왜 이 어노테이션이 동작하지?" / "왜 안 되지?" 디버깅이 빨라진다.

---

## 전체 흐름 다이어그램

```
HTTP Request
    │
    ▼
┌──────────────────────────────────────────────────────────────┐
│ Servlet Container (Jetty 12)                                 │
│                                                              │
│  Filter Chain  ─────────────────────────  (filter-vs-interceptor.md)
│       │                                                      │
│       ▼                                                      │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ DispatcherServlet                                       │  │
│  │                                                         │  │
│  │   1. HandlerMapping                                     │  │
│  │      (URL + method → HandlerMethod 매핑)                │  │
│  │            │                                            │  │
│  │            ▼                                            │  │
│  │   2. HandlerInterceptor#preHandle  (별도 문서)          │  │
│  │            │                                            │  │
│  │            ▼                                            │  │
│  │   3. HandlerAdapter (메서드 실제 호출)                  │  │
│  │      ├─ @RequestBody  → HttpMessageConverter (body→객체) │  │
│  │      ├─ @RequestParam / @PathVariable / @RequestHeader  │  │
│  │      ├─ Validation (@Valid)                             │  │
│  │      ├─ 컨트롤러 메서드 호출                            │  │
│  │      └─ 반환값 → HttpMessageConverter (객체→body)        │  │
│  │            │                                            │  │
│  │            ▼                                            │  │
│  │   4. HandlerInterceptor#postHandle                      │  │
│  │            │                                            │  │
│  │            ▼                                            │  │
│  │   5. View Resolution                                    │  │
│  │      (@RestController는 이 단계 skip)                   │  │
│  │            │                                            │  │
│  │            ▼                                            │  │
│  │   6. HandlerInterceptor#afterCompletion                 │  │
│  └────────────────────────────────────────────────────────┘  │
│       │                                                      │
│       ▼                                                      │
│  Filter Chain (응답)                                          │
└──────────────────────────────────────────────────────────────┘
    │
    ▼
HTTP Response
```

핵심 관전 포인트:
- **HandlerMapping**: "이 URL은 어느 컨트롤러 메서드가 처리할지" 결정. `RequestMappingHandlerMapping`이 `@RequestMapping` 계열을 스캔해 라우팅 테이블을 만든다.
- **HandlerAdapter**: 결정된 메서드를 실제로 호출하는 책임. `RequestMappingHandlerAdapter`가 파라미터 바인딩, 검증, 반환값 변환을 다 처리한다.
- **HttpMessageConverter**: HTTP body ↔ Kotlin/Java 객체 변환. `@RequestBody`로 들어오는 요청, 그리고 반환값을 응답 body로 직렬화할 때 모두 사용된다.

---

## 핸들러 어노테이션

### `@Controller` vs `@RestController`

| | 반환값 처리 | 언제 |
| --- | --- | --- |
| `@Controller` | View 이름으로 해석 (Thymeleaf, JSP 등) | 서버 사이드 렌더링 |
| `@RestController` | **body로 직접 직렬화** (`= @Controller + @ResponseBody`) | REST API (현재 모듈) |

### `@RequestMapping`과 단축형

```kotlin
@GetMapping("/users/{id}")    // = @RequestMapping(method = [GET], path = ["/users/{id}"])
@PostMapping("/users")
@PutMapping("/users/{id}")
@DeleteMapping("/users/{id}")
@PatchMapping("/users/{id}")
```

클래스에 `@RequestMapping("/api/echo")`를 붙이면 메서드 경로에 prefix가 붙는다.

### 메서드 파라미터 바인딩

| 어노테이션 | 어디서 옴 |
| --- | --- |
| `@PathVariable` | URL 경로의 `{id}` 부분 |
| `@RequestParam` | 쿼리스트링 (`?msg=...`) 또는 form-urlencoded |
| `@RequestHeader` | HTTP 헤더 |
| `@RequestBody` | HTTP body (JSON 등). `HttpMessageConverter` 통과 |
| `@CookieAttribute` | 쿠키 |
| `@ModelAttribute` | form 데이터 또는 쿼리 → 객체 |

옵션:
- `required = false` + Kotlin nullable 타입 → 없으면 `null`
- `defaultValue = "..."` → 없을 때 기본값 (자동으로 required=false)

---

## HttpMessageConverter

HTTP body와 객체 사이의 변환 담당. Spring Boot가 classpath를 보고 자동 등록한다. 이 모듈 기준 등록되는 주요 컨버터:

| 컨버터 | 무엇을 처리 |
| --- | --- |
| `MappingJackson2HttpMessageConverter` | `application/json` ↔ 모든 Kotlin/Java 객체. **이게 90%** |
| `StringHttpMessageConverter` | `text/plain` ↔ `String` |
| `ByteArrayHttpMessageConverter` | `application/octet-stream` ↔ `ByteArray` |
| `FormHttpMessageConverter` | `application/x-www-form-urlencoded` ↔ `MultiValueMap` |
| `ResourceHttpMessageConverter` | 파일 다운로드 등 |

### `jackson-module-kotlin`의 역할

Kotlin data class는 자바 Bean 컨벤션(파라미터 없는 기본 생성자 + setter)을 만족하지 않는다. `jackson-module-kotlin`이 없으면 Jackson이 역직렬화에 실패한다. 이 모듈의 `build.gradle.kts`엔 이미 의존성이 들어 있어 자동 등록 → val data class가 그대로 동작.

### 어떻게 선택되나

- **요청** 쪽: 요청의 `Content-Type` 헤더 + 파라미터 타입을 보고 매칭되는 컨버터를 선택.
- **응답** 쪽: 요청의 `Accept` 헤더 + 반환 타입 + 컨트롤러의 `produces` 속성을 종합해 선택.

매칭되는 컨버터가 없으면:
- 요청: `415 Unsupported Media Type`
- 응답: `406 Not Acceptable`

---

## Content Negotiation

**같은 URL을 다양한 형식(JSON, XML, plain text 등)으로 응답할 수 있게 하는 메커니즘.** Spring Boot 기본 정책은 **`Accept` 헤더만** 본다. (옛날에는 URL 확장자나 `?format=json` 파라미터도 봤지만, Spring Framework 5.3 이후로는 보안 이슈로 기본 비활성화.)

### 패턴 1 — 메서드 두 개로 분리

```kotlin
@GetMapping("/{id}/preview", produces = [MediaType.APPLICATION_JSON_VALUE])
fun previewJson(@PathVariable id: Long): EchoResponse = ...

@GetMapping("/{id}/preview", produces = [MediaType.TEXT_PLAIN_VALUE])
fun previewText(@PathVariable id: Long): String = ...
```

Spring이 `Accept` 헤더를 보고 매칭되는 쪽을 선택한다. 다른 모든 Accept는 406.

### 패턴 2 — 하나의 메서드 + 컨버터에 위임

```kotlin
@GetMapping("/{id}/preview")
fun preview(@PathVariable id: Long): EchoResponse = ...
```

반환 타입이 객체라면 등록된 컨버터들 중에 `Accept`와 매칭되는 게 있을 때만 변환. 보통 JSON만 등록돼 있어 사실상 JSON 전용.

### XML도 지원하고 싶다면

`jackson-dataformat-xml` 의존성을 추가하면 `MappingJackson2XmlHttpMessageConverter`가 자동 등록되어 같은 컨트롤러가 별도 코드 없이 XML 응답도 한다.

---

## `ResponseEntity` — 언제 쓰나

**평범하게 200 OK + body만 내려 보낸다면 객체를 그대로 반환하는 게 간단**:

```kotlin
@GetMapping("/users/{id}")
fun get(@PathVariable id: Long): User = userService.find(id)
```

**상태 코드를 바꾸거나, 헤더를 명시하고 싶을 때만** `ResponseEntity`:

```kotlin
@DeleteMapping("/{id}")
fun delete(@PathVariable id: Long): ResponseEntity<Void> =
    ResponseEntity.noContent()                        // 204
        .header("X-Deleted-Id", id.toString())
        .build()
```

흔한 빌더들:
- `ResponseEntity.ok(body)` — 200
- `ResponseEntity.created(URI).body(...)` — 201 + Location
- `ResponseEntity.noContent().build()` — 204
- `ResponseEntity.status(HttpStatus.X).body(...)` — 임의

> 응답 상태 코드를 메서드 위에 어노테이션으로 박고 싶다면 `@ResponseStatus(HttpStatus.CREATED)`도 있다. 단, 헤더까지 다루려면 `ResponseEntity`가 유연하다.

---

## 이 모듈의 예제 코드

| 파일 | 역할 |
| --- | --- |
| [`learning/mvc/EchoModels.kt`](../../src/main/kotlin/me/victor/spring01/learning/mvc/EchoModels.kt) | 요청/응답 data class (Jackson + kotlin-module로 자동 직렬화) |
| [`learning/mvc/EchoController.kt`](../../src/main/kotlin/me/victor/spring01/learning/mvc/EchoController.kt) | `@RestController` + `@RequestMapping`, 모든 핵심 어노테이션 + content negotiation + `ResponseEntity` |

### curl로 실행해 확인

```bash
./gradlew :01-mvc-basics:bootRun
```

```bash
# 1. 쿼리스트링 + 기본값
curl 'http://localhost:8080/api/echo?msg=hi&times=3'
# {"echo":"hihihi","length":6,"receivedAt":"..."}

# 2. 경로변수 + 헤더
curl -H 'X-User-Agent-Note: studying' 'http://localhost:8080/api/echo/42'
# {"id":42,"note":"studying","echo":"echoing #42"}

# 3. Content Negotiation — JSON
curl -H 'Accept: application/json' 'http://localhost:8080/api/echo/42/preview'
# {"echo":"preview json for #42","length":0,"receivedAt":"..."}

# 3'. Content Negotiation — plain text
curl -H 'Accept: text/plain' 'http://localhost:8080/api/echo/42/preview'
# preview text for #42

# 3''. Content Negotiation — XML (등록 컨버터 없음 → 406)
curl -i -H 'Accept: application/xml' 'http://localhost:8080/api/echo/42/preview'
# HTTP/1.1 406 Not Acceptable

# 4. JSON body POST
curl -X POST -H 'Content-Type: application/json' \
    -d '{"message":"echo","repeat":2}' \
    http://localhost:8080/api/echo
# {"echo":"echoecho","length":8,"receivedAt":"..."}

# 5. ResponseEntity — 상태 204 + 커스텀 헤더
curl -i -X DELETE 'http://localhost:8080/api/echo/99'
# HTTP/1.1 204 No Content
# X-Deleted-Id: 99
# Cache-Control: no-store
```

---

## 자주 빠지는 함정

1. **`@Controller`로 REST API를 만든다** — View 이름으로 해석되어 404나 이상한 동작. `@RestController` 또는 메서드마다 `@ResponseBody`.
2. **`@RequestParam`과 `@PathVariable`을 혼동** — 쿼리(`?id=1`) vs 경로(`/{id}`). 헷갈리면 URL 형태부터 확인.
3. **JSON body가 안 받아진다** — `Content-Type: application/json`을 보내지 않아서. curl에서 `-H 'Content-Type: application/json'`을 잊는 게 가장 흔함.
4. **Kotlin data class가 역직렬화 실패** — `jackson-module-kotlin` 누락. 이 모듈은 포함돼 있지만 새 모듈 만들 때 다시 확인.
5. **`ResponseEntity<Unit>`를 빈 body로 의도** — Kotlin의 `Unit`은 JSON으로 `{}`로 직렬화된다. 진짜 빈 응답을 원하면 `ResponseEntity<Void>` + `.build()`.
6. **응답 상태 코드를 바꾸려고 `HttpServletResponse#setStatus`를 직접 호출** — Spring이 컨버터/예외 처리 단계에서 덮어쓸 수 있음. `ResponseEntity` 또는 `@ResponseStatus`가 안전.
7. **컨트롤러에 도메인 엔티티(`@Entity`)를 직접 노출** — 변경 결합도 ↑ + 양방향 연관관계가 직렬화 무한 루프. **항상 DTO로 분리**.

---

## 다음

- Tier 1 #3 — **Validation + `ProblemDetail`** (입력 검증과 RFC 9457 표준 에러 응답)
