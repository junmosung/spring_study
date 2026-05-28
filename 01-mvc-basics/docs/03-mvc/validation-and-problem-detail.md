# Validation + `ProblemDetail` (RFC 9457)

> Tier 1 #3 — 입력 검증과 표준화된 에러 응답. **실무 API 품질의 80%가 여기서 갈린다.**

JSON으로 들어온 값을 안 믿고 한 번 더 검사해서 잘못된 요청을 빠르게 거부하고, 거부할 때는 클라이언트가 파싱하기 쉬운 표준 포맷으로 응답한다.

---

## 두 단계로 본다

```
[ Client → Server ]
           │
           ▼
   1) Jakarta Bean Validation     ← @Valid / @Validated + jakarta 어노테이션
           │
      검증 실패시
           │
           ▼
   2) @RestControllerAdvice       ← 예외를 ProblemDetail로 변환
           │
           ▼
   application/problem+json (RFC 9457)
```

---

## Jakarta Bean Validation

### 주요 어노테이션 카탈로그

| 카테고리 | 어노테이션 |
| --- | --- |
| 존재 | `@NotNull`, `@NotBlank`(문자열 빈값/공백 금지), `@NotEmpty`(컬렉션) |
| 크기 | `@Size(min, max)`, `@Min`, `@Max`, `@Positive`, `@Negative` |
| 패턴 | `@Pattern(regexp)`, `@Email` |
| 시간 | `@Past`, `@Future`, `@PastOrPresent`, `@FutureOrPresent` |
| 숫자 | `@Digits(integer, fraction)`, `@DecimalMin`, `@DecimalMax` |
| 중첩 | `@Valid` — 필드의 객체 안쪽까지 재귀 검증 |

각 어노테이션에 `message = "..."` 로 실패 메시지 커스터마이즈.

### `@Valid` vs `@Validated`

| | 표준 | 위치 | 용도 |
| --- | --- | --- | --- |
| `@Valid` | jakarta | `@RequestBody`, 필드, 메서드 파라미터 | body 전체 또는 중첩 객체 cascading 검증 |
| `@Validated` | **Spring** | **클래스 레벨**, 메서드 파라미터 | path/query 인자 검증 활성화, validation group 지정 |

→ 컨트롤러에서 `@PathVariable` / `@RequestParam`에 jakarta 어노테이션을 붙여도, **클래스에 `@Validated`가 없으면 검증이 그냥 무시된다**. 가장 흔한 함정.

### Kotlin 어노테이션 타깃

```kotlin
// 보통 코드 (다른 모듈)
data class Req(@field:NotBlank val name: String)

// 이 모듈 — @field: 없이 OK
data class Req(@NotBlank val name: String)
```

`build.gradle.kts`의 컴파일러 옵션 `-Xannotation-default-target=param-property` 덕분에 val 파라미터의 어노테이션이 property(getter)에도 적용된다. 이 모듈에서는 `@field:` 접두 없이도 jakarta validation이 동작.

---

## 검증 실패 시 발생하는 예외

| 예외 | 언제 | 어떻게 잡나 |
| --- | --- | --- |
| `MethodArgumentNotValidException` | `@Valid @RequestBody` 실패 | `@ExceptionHandler` |
| `HandlerMethodValidationException` | Spring 6.1+ 메서드 인자 검증 실패 (현대 경로) | `@ExceptionHandler` |
| `ConstraintViolationException` | 일부 환경(서비스 메서드, 구 경로 등)에서 발생 | `@ExceptionHandler` |
| `HttpMessageNotReadableException` | JSON 자체가 깨졌거나 타입 불일치 | 기본 핸들러가 처리 |

세 검증 예외 모두 한 핸들러에서 잡아 비슷한 ProblemDetail로 변환하는 게 일관적.

---

## `ProblemDetail` (RFC 9457)

**HTTP API 에러 응답 표준 포맷**. RFC 7807(2016)을 갱신해 2023년 7월 RFC 9457로 다시 발행. Spring 6.0+ 가 `org.springframework.http.ProblemDetail`로 1급 지원.

### 기본 필드

| 필드 | 의미 | 예 |
| --- | --- | --- |
| `type` | 에러 종류를 식별하는 URI (영구 링크 권장) | `https://example.com/errors/user-not-found` |
| `title` | 사람이 읽는 짧은 요약 | `"User Not Found"` |
| `status` | HTTP 상태 코드 | `404` |
| `detail` | 이 발생 인스턴스에 대한 설명 | `"User 404 not found"` |
| `instance` | 이 발생을 식별하는 URI (보통 요청 path) | `/api/users/404` |
| 임의 확장 | `setProperty("...", value)` 로 추가 가능 | `errors`, `userId`, `traceId` 등 |

### 예시 응답

```http
HTTP/1.1 404 Not Found
Content-Type: application/problem+json

{
  "type":     "https://example.com/errors/user-not-found",
  "title":    "User Not Found",
  "status":   404,
  "detail":   "User 404 not found",
  "instance": "/api/users/404",
  "userId":   404
}
```

### Spring Boot에서 활성화

```yaml
spring:
  mvc:
    problemdetails:
      enabled: true
```

이 모듈 `application.yaml`에 이미 켜져 있다. 켜지면 우리가 직접 핸들링하지 않은 Spring 내장 예외(예: `HttpRequestMethodNotSupportedException`, `HttpMediaTypeNotSupportedException`)도 자동으로 ProblemDetail로 응답된다.

---

## 글로벌 에러 처리

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException::class)
    fun handleNotFound(ex: UserNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message!!).apply {
            title = "User Not Found"
            type = URI.create("https://example.com/errors/user-not-found")
            setProperty("userId", ex.id)
        }
}
```

- `@RestControllerAdvice` = `@ControllerAdvice + @ResponseBody`. 반환한 `ProblemDetail`이 자동으로 `application/problem+json`으로 직렬화.
- `@ExceptionHandler`는 클래스의 가장 구체적인 타입부터 매칭된다 (다형성 고려).
- 도메인 예외 → HTTP 상태 변환은 **web 계층의 책임**. 도메인 예외 클래스에 HTTP 코드를 박지 말 것.

### 확장 핸들러 — 필드별 에러 추가

```kotlin
@ExceptionHandler(MethodArgumentNotValidException::class)
fun handleBodyValidation(ex: MethodArgumentNotValidException): ProblemDetail =
    ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "...").apply {
        setProperty("errors", ex.bindingResult.fieldErrors.map {
            mapOf("field" to it.field, "message" to it.defaultMessage, "rejected" to it.rejectedValue)
        })
    }
```

→ 응답 body에 `errors` 배열이 포함돼 클라이언트가 어떤 필드가 왜 거부됐는지 정확히 알 수 있다.

---

## 흐름 다이어그램

```
Client Request
     │  (JSON body or query/path)
     ▼
┌───────────────────────────────┐
│ HandlerAdapter                │
│  ├─ HttpMessageConverter      │  body → DTO 객체
│  ├─ Jakarta Validation 실행  │  @Valid / @Validated
│  │     │                      │
│  │     ▼                      │
│  │  검증 실패?                │
│  │     │YES                   │
│  │     ▼                      │
│  │  throw 검증 예외           │ ─┐
│  │                            │  │
│  └─ 컨트롤러 메서드 호출      │  │
│         │                     │  │
│         ▼                     │  │
│      throw 도메인 예외         │ ─┤
│         │                     │  │
│         ▼                     │  │
│      정상 반환                │  │
└────────┬──────────────────────┘  │
         │                         │
         ▼                         ▼
   HttpMessageConverter      @RestControllerAdvice
   객체 → JSON 직렬화        @ExceptionHandler
         │                    예외 → ProblemDetail
         ▼                         │
   200/201/204 응답              4xx/5xx
                                   │
                                   ▼
                          application/problem+json
```

---

## 이 모듈의 예제 코드

| 파일 | 역할 |
| --- | --- |
| [`learning/validation/UserModels.kt`](../../src/main/kotlin/me/victor/spring01/learning/validation/UserModels.kt) | `CreateUserRequest` + `AddressDto`(중첩) + `UserResponse` |
| [`learning/validation/UserController.kt`](../../src/main/kotlin/me/victor/spring01/learning/validation/UserController.kt) | `@Validated` 클래스 + 4가지 검증 트리거 위치 |
| [`learning/validation/DomainExceptions.kt`](../../src/main/kotlin/me/victor/spring01/learning/validation/DomainExceptions.kt) | `UserNotFoundException` |
| [`learning/validation/GlobalExceptionHandler.kt`](../../src/main/kotlin/me/victor/spring01/learning/validation/GlobalExceptionHandler.kt) | `@RestControllerAdvice`로 4종 예외 → ProblemDetail |
| [`src/main/resources/application.yaml`](../../src/main/resources/application.yaml) | `spring.mvc.problemdetails.enabled=true` |

### curl로 실행해 확인

```bash
./gradlew :01-mvc-basics:bootRun
```

```bash
# 정상 — 201/200 응답
curl -X POST -H 'Content-Type: application/json' \
    -d '{"username":"victor","email":"v@example.com","age":25}' \
    http://localhost:8080/api/users
# {"id":1,"username":"victor","email":"v@example.com","age":25}

# 1. body validation 실패 (3개 필드 동시 위반)
curl -i -X POST -H 'Content-Type: application/json' \
    -d '{"username":"ab","email":"not-an-email","age":10}' \
    http://localhost:8080/api/users
# HTTP/1.1 400 Bad Request
# Content-Type: application/problem+json
# {
#   "type":   "about:blank",
#   "title":  "Validation Failed",
#   "status": 400,
#   "detail": "요청 본문 검증에 실패했습니다.",
#   "instance": "/api/users",
#   "errors": [
#     {"field":"username","message":"username은 3~20자여야 합니다.","rejected":"ab"},
#     {"field":"email","message":"올바른 이메일 형식이어야 합니다.","rejected":"not-an-email"},
#     {"field":"age","message":"14세 이상이어야 합니다.","rejected":10}
#   ]
# }

# 2. 중첩 검증 (address.city 빈값)
curl -i -X POST -H 'Content-Type: application/json' \
    -d '{"username":"victor","email":"v@example.com","age":25,"address":{"city":"","street":"main"}}' \
    http://localhost:8080/api/users
# errors[].field = "address.city"

# 3. PathVariable 검증 (id < 1)
curl -i 'http://localhost:8080/api/users/0'
# 400, errors[].parameter = "id"

# 4. RequestParam 검증 (이메일 형식 위반)
curl -i 'http://localhost:8080/api/users/search?email=not-an-email'
# 400, errors[].parameter = "email"

# 5. 도메인 예외 → 404 ProblemDetail
curl -i 'http://localhost:8080/api/users/404'
# HTTP/1.1 404 Not Found
# Content-Type: application/problem+json
# {
#   "type":    "https://example.com/errors/user-not-found",
#   "title":   "User Not Found",
#   "status":  404,
#   "detail":  "User 404 not found",
#   "instance":"/api/users/404",
#   "userId":  404
# }

# 6. 다루지 않은 예외 (잘못된 HTTP 메서드) → Spring 기본이 ProblemDetail
curl -i -X PUT 'http://localhost:8080/api/users/1'
# HTTP/1.1 405 Method Not Allowed
# Content-Type: application/problem+json
# (Spring이 자동 변환, problemdetails.enabled=true 덕분)
```

---

## 자주 빠지는 함정

1. **`@Validated`를 클래스에 안 붙임** — `@PathVariable @Min(1)` 같은 게 그냥 무시된다. 검증이 안 도는 가장 흔한 원인.
2. **`@Valid`와 `@Validated`를 혼동** — body는 `@Valid`, 클래스에는 `@Validated`. 둘 다 필요한 경우가 많다.
3. **중첩 객체의 `@Valid`를 잊는다** — 외부 DTO에 `@Valid`만 붙이고 그 안 필드의 객체에 `@Valid`가 없으면 cascading 안 됨.
4. **도메인 예외에 HTTP 상태 코드를 박는다** — 도메인은 비즈니스 의미만, 상태 변환은 web 계층 책임.
5. **에러 응답을 핸들러마다 다르게 만든다** — 클라이언트가 파싱을 5가지로 분기해야 함. 한 핸들러 클래스에서 ProblemDetail로 일관화.
6. **`spring.mvc.problemdetails.enabled`를 안 켜고 기본 응답이 RFC와 다르다고 당황** — Spring Boot의 기본 errors 응답과 ProblemDetail은 별개. 설정 켤 것.
7. **검증 메시지 i18n을 잊는다** — `message = "...."` 직접 박지 말고 `message = "{user.username.size}"` + `messages.properties`로 분리하면 다국어 가능. (이 모듈에선 단순화)
8. **`@Validated`로 validation group 안 활용** — 같은 DTO를 create/update에 같이 쓸 때 그룹을 나눠 다른 검증 규칙 적용 가능.

---

## 다음

- Tier 1 #4 — **Configuration & Profiles** (이미 #1-c에서 properties는 다뤘으니 여기선 profile별 설정 / 활성화 / SpringApplication 진입점 옵션 위주)
