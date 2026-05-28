# Filter → DispatcherServlet → Response — 전체 흐름 심화

> **이 문서의 자리**:
> - [`filter-vs-interceptor.md`](./filter-vs-interceptor.md) — 한눈 비교(개관)
> - [`mvc-request-flow.md`](./mvc-request-flow.md) — 어노테이션/컨버터/Content Negotiation 등 **API 표면**
> - **이 문서** — `doFilter` 타이밍부터 DispatcherServlet 내부 7단계까지 **메커니즘** 중심 심화

---

## Part 1. Filter 단계

### 1.1 `doFilter`는 언제 호출되는가

Servlet Container(Jetty 12)가 요청 소켓을 받아 `HttpServletRequest`를 만든 직후, **DispatcherServlet에 진입하기 전**. 즉 *컨트롤러는 물론, HandlerMapping이 돌아가기도 전*.

```kotlin
override fun doFilterInternal(req, res, chain) {
    // [A] ─── "컨트롤러 들어가기 전"
    //         DispatcherServlet조차 아직 안 돌았다.
    //         req.userPrincipal == null, req.getParameter()도 아직 안 됨.

    chain.doFilter(req, res)
    //  ↑ 이 한 줄이 호출되는 순간, 안쪽 전부가 동기적으로 실행된다:
    //   → 다음 Filter들
    //   → DispatcherServlet (Part 2 전체)
    //   → 다음 Filter들의 "after" 구간
    //  여기까지 다 끝나야 chain.doFilter()가 return 한다.

    // [B] ─── "컨트롤러까지 다 끝난 후, 응답 나가기 직전"
    //         response.status, 본문 길이 등을 모두 알 수 있다.
}
```

→ Filter는 **요청을 보내기 전(A) + 응답이 돌아온 후(B)** 모두에 끼어들 수 있는, 일종의 "**감싸는 try/finally**" 구조.

### 1.2 전체 흐름도

```
                            Client (브라우저/cURL)
                                  │  TCP/HTTP
                                  ▼
        ┌───────────────────────────────────────────────────────────────┐
        │  Servlet Container (Jetty 12)                                 │
        │  • TCP 수락, HTTP 파싱, HttpServletRequest/Response 생성       │
        │                                                               │
        │  ┌─────────────────────────────────────────────────────────┐  │
        │  │  Filter Chain                ◀── [A] doFilter 진입 전   │  │
        │  │                                                         │  │
        │  │  ① CharacterEncodingFilter   (encoding 결정)            │  │
        │  │  ② CorsFilter / SecurityFilterChain  (인증/차단)        │  │
        │  │  ③ RequestLoggingFilter      (← 우리 예제)              │  │
        │  │  ④ ShallowEtagHeaderFilter   (캐시)                     │  │
        │  │  ⑤ CompressingFilter         (GZip)                     │  │
        │  │                                                         │  │
        │  │            ↓ chain.doFilter()                          │  │
        │  │  ┌────────────────────────────────────────────────┐    │  │
        │  │  │  DispatcherServlet (Part 2에서 깊이 다룸)      │    │  │
        │  │  │                                                │    │  │
        │  │  │  1. HandlerMapping      → HandlerExecutionChain│    │  │
        │  │  │  2. Interceptor.preHandle                      │    │  │
        │  │  │  3. HandlerAdapter      → @Controller 호출      │    │  │
        │  │  │     (ArgumentResolver / Validation / Invoke /  │    │  │
        │  │  │      ReturnValueHandler / MessageConverter)     │    │  │
        │  │  │  4. Interceptor.postHandle                     │    │  │
        │  │  │  5. View 렌더링 (REST면 skip)                  │    │  │
        │  │  │  6. Interceptor.afterCompletion                │    │  │
        │  │  │                                                │    │  │
        │  │  │  예외 시: HandlerExceptionResolver             │    │  │
        │  │  │          → @ControllerAdvice 등                │    │  │
        │  │  └────────────────────────────────────────────────┘    │  │
        │  │            ↑ chain.doFilter() return                   │  │
        │  │                                                         │  │
        │  │  ⑤ ④ ③ ② ①  ◀── [B] 응답 가공 / 로깅 (역순)            │  │
        │  └─────────────────────────────────────────────────────────┘  │
        └───────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
                            Client (응답)
```

### 1.3 Filter vs Interceptor — 결정적 차이

| 기준 | Filter | Interceptor |
|---|---|---|
| **계층** | Servlet 스펙(컨테이너 레벨) | Spring MVC(DispatcherServlet 안) |
| **핸들러 정보** | ❌ 모름 | ✅ `HandlerMethod`로 어떤 컨트롤러인지 안다 |
| **req/res 래핑** | ✅ Wrapper로 본문/스트림 교체 가능 | ❌ 이미 만들어진 그대로 사용 |
| **404/매핑실패** | ✅ 본다 | ❌ 안 본다(HandlerMapping이 못 찾았기 때문) |
| **정적 리소스** | ✅ 본다 | ⚠️ ResourceHttpRequestHandler만 본다 |

### 1.4 각 횡단 관심사가 *왜 Filter여야 하는지*

#### ① 공통 액세스 로깅

404, 405, Security가 차단한 요청까지 *모두* 로깅하려면 DispatcherServlet 바깥에서 잡아야 합니다. Interceptor는 HandlerMapping이 핸들러를 찾아낸 요청만 봅니다.

```
GET /존재하지않는경로
  ├─ Filter: "→ GET /존재하지않는경로"     ✅ 잡힌다
  ├─ DispatcherServlet → HandlerMapping: no match → 404
  ├─ Interceptor.preHandle:                ❌ 호출 안 됨
  └─ Filter: "← GET /... status=404"      ✅ 잡힌다
```

#### ② CORS

**(a) Preflight `OPTIONS` 요청**은 컨트롤러에 도달할 필요가 없음 — 헤더만 보고 즉시 응답:

```
Browser: OPTIONS /api/users
         Access-Control-Request-Method: POST
         Origin: https://other-domain.com
                   │
                   ▼
   ┌─ CorsFilter ─────────────────────┐
   │  허용된 Origin인지 검사           │
   │  → 즉시 200 (헤더만 채워서)       │  ✅ 컨트롤러까지 안 감
   │  → 차단되면 403                  │
   └──────────────────────────────────┘
```

**(b) CORS 헤더는 *에러 응답에도* 붙어야** 함. 컨트롤러가 500을 던져도 `Access-Control-Allow-Origin` 헤더가 없으면 브라우저가 응답 자체를 차단합니다.

#### ③ GZip 압축

**응답 OutputStream을 통째로 갈아끼우는** 일. 컨트롤러가 `write(...)` 하기 *전에* Wrapper를 끼워둬야 함. Interceptor는 이미 만들어진 response를 받기 때문에 스트림 교체가 불가능.

```kotlin
class CompressingFilter : OncePerRequestFilter() {
    override fun doFilterInternal(req, res, chain) {
        if (acceptsGzip(req)) {
            val wrapped = GZipResponseWrapper(res)   // ◀── OutputStream을 교체한 Wrapper
            chain.doFilter(req, wrapped)             // ◀── 컨트롤러는 모르고 그냥 write
            wrapped.finishGzip()
        } else {
            chain.doFilter(req, res)
        }
    }
}
```

#### ④ 문자 인코딩 (`CharacterEncodingFilter`)

`request.setCharacterEncoding("UTF-8")`은 **본문이 파싱되기 전에** 호출돼야 효과가 있습니다. DispatcherServlet은 핸들러를 찾으면서 본문/파라미터를 읽기 시작 — 이미 읽으면 인코딩 결정이 굳어버림.

```
[Filter 단계]                  [DispatcherServlet 단계]
req.setCharacterEncoding(UTF-8)  →  @RequestBody body  ──read──▶ ✅ UTF-8로 파싱

[만약 Interceptor에서 호출하면]
DispatcherServlet → HandlerMapping → @RequestBody body ──read──▶ ❌ 기본 인코딩으로 깨짐
                          │
                          ▼
                   Interceptor.preHandle (setCharacterEncoding) ← 의미 없음, 이미 읽음
```

#### ⑤ 인증 토큰 추출 (Spring Security)

보안 차단은 **컨트롤러 도달 전에** 끝나야 합니다. Spring Security는 *통째로* Filter Chain 위에 구현돼 있어요(`SecurityFilterChain`). 또 인증은 정적 리소스 등 *컨트롤러가 없는 URL에도 적용*돼야 하는데, Interceptor는 그런 경로엔 못 끼어듭니다.

```
요청
  │
  ▼
┌─ Filter: JwtAuthenticationFilter ───────────────┐
│  Header에서 "Authorization: Bearer xxx" 추출    │
│  ├─ 유효 → SecurityContextHolder.set(인증객체)  │
│  └─ 무효 → 401 즉시 응답                         │  ✅ 컨트롤러 호출 안 됨
└──────────────────────────────────────────────────┘
  │
  ▼
DispatcherServlet → Controller (이미 인증된 상태)
```

### 1.5 Part 1 판단 기준

> **"DispatcherServlet 밖의 일까지 알아야 하나? 본문/스트림을 바꿔야 하나? 컨트롤러 도달 전에 막아야 하나?"**
> 중 하나라도 yes면 Filter. 셋 다 no면 Interceptor가 자연스럽다.

---

## Part 2. DispatcherServlet 내부 깊이 보기

`chain.doFilter(req, res)`가 호출되면 결국 **`DispatcherServlet#doDispatch(...)`** 가 호출됩니다. 이 메서드 한 개가 사실상 Spring MVC의 심장입니다. 안에서 일어나는 일을 순서대로 풉니다.

### 2.0 DispatcherServlet의 정체

- **Front Controller 패턴**의 구현체. 모든 HTTP 요청의 단일 진입점.
- `HttpServlet`을 상속. Spring Boot가 자동으로 `"/"` 경로에 매핑해 등록.
- 자신은 *직접 처리하지 않고*, 7가지 협력자(`HandlerMapping`, `HandlerAdapter`, `HandlerExceptionResolver`, `ViewResolver`, `LocaleResolver`, `ThemeResolver`, `MultipartResolver`)에게 위임.
- 이 협력자들은 모두 **Spring Bean으로 교체 가능** — 그래서 MVC가 그렇게 유연한 것.

```
HttpServlet.service()
    └─ doGet/doPost/...
        └─ FrameworkServlet.processRequest()
            └─ DispatcherServlet.doService()
                └─ DispatcherServlet.doDispatch()  ◀── 여기가 핵심
```

### 2.1 단계 1 — HandlerMapping (라우팅)

**역할**: 요청의 URL + HTTP method + 헤더 등을 보고, "어떤 `HandlerMethod`(=`@Controller`의 메서드)가 처리할지" 결정.

**기본 구현**: `RequestMappingHandlerMapping`
- 애플리케이션 시작 시점에 모든 `@RequestMapping`/`@GetMapping`/...을 스캔
- `RequestMappingInfo`(경로 패턴 + method + params + headers + consumes + produces) → `HandlerMethod`(bean + method)의 라우팅 테이블을 메모리에 구축
- 요청이 올 때마다 이 테이블에서 lookup

**반환**: `HandlerExecutionChain` = `HandlerMethod` + 적용 가능한 `HandlerInterceptor` 리스트

```kotlin
// DispatcherServlet.doDispatch 내부의 단순화된 의사 코드
val mappedHandler: HandlerExecutionChain = getHandler(request)
    ?: run { noHandlerFound(request, response); return }   // ← 여기서 404
```

**핵심 디테일**:
- 매칭 실패 시 `noHandlerFound` → 기본 동작은 404 응답
- 정적 리소스 요청은 `SimpleUrlHandlerMapping` + `ResourceHttpRequestHandler`가 매핑함
- `@RestController`도 결국 `@Controller` + `@ResponseBody`라 동일한 매핑 로직을 탐

### 2.2 단계 2 — `HandlerInterceptor#preHandle`

라우팅이 성공했을 때만 호출. 등록 순서대로 차례차례 실행:

```kotlin
for (interceptor in mappedHandler.interceptors) {
    if (!interceptor.preHandle(request, response, handler)) {
        // false 반환 시: 체인 중단. 컨트롤러 호출 안 됨.
        // 이미 preHandle이 끝난 interceptor들의 afterCompletion만 역순으로 호출.
        triggerAfterCompletion(...)
        return
    }
}
```

**여기서 가능한 것 vs Filter에서 가능한 것**:
- ✅ `handler`가 `HandlerMethod`로 캐스팅되어 **어떤 메서드인지 안다** — `handler.getMethodAnnotation(Secured::class.java)`로 메서드 단위 권한 검사 가능
- ❌ 요청/응답을 Wrapper로 갈아끼울 수는 없음 (이미 DispatcherServlet 안)

### 2.3 단계 3 — HandlerAdapter (메서드 실제 호출)

**역할**: `HandlerMethod`를 *실제로 호출*하기 위한 모든 일.

**기본 구현**: `RequestMappingHandlerAdapter`. 이게 일하는 순서:

```
HandlerAdapter.handle(request, response, handler)
   │
   ├─ (a) ArgumentResolver 체인이 각 파라미터 값을 만든다
   │       - @PathVariable, @RequestParam, @RequestHeader, @CookieValue
   │       - @RequestBody → HttpMessageConverter로 body 역직렬화
   │       - @ModelAttribute → 폼 데이터 → 객체 + 바인딩
   │       - HttpServletRequest, HttpSession 같은 인프라 타입도 여기서 주입
   │
   ├─ (b) Validation (@Valid가 붙어 있으면 Jakarta Bean Validation 호출)
   │       - 실패 시 MethodArgumentNotValidException 발생 → 단계 5'(예외 경로)
   │
   ├─ (c) Method.invoke(controllerBean, args)   ← 진짜 컨트롤러 호출
   │
   └─ (d) ReturnValueHandler 체인이 반환값을 처리
           - @ResponseBody / @RestController → HttpMessageConverter로 응답 body 직렬화
           - ResponseEntity → 상태/헤더/본문 분리해서 처리
           - ModelAndView / String(view 이름) → 단계 5(View 렌더링)로 전달
           - void → ModelAndView=null → 후속 단계가 알아서 처리
```

#### (a) ArgumentResolver 디테일

각 파라미터는 등록된 `HandlerMethodArgumentResolver` 중 *처음으로 `supportsParameter()`가 true*인 것이 처리. 주요 resolver:

| Resolver | 처리 대상 |
|---|---|
| `PathVariableMethodArgumentResolver` | `@PathVariable` |
| `RequestParamMethodArgumentResolver` | `@RequestParam`, *어노테이션 없는 단순 타입* |
| `RequestHeaderMethodArgumentResolver` | `@RequestHeader` |
| `RequestResponseBodyMethodProcessor` | `@RequestBody`, `@ResponseBody` 동시 처리 |
| `ServletRequestMethodArgumentResolver` | `HttpServletRequest`, `Locale`, `Principal` 등 |
| `ModelAttributeMethodProcessor` | `@ModelAttribute`, *어노테이션 없는 복합 타입* |

→ "왜 `@RequestParam` 없이도 쿼리 파라미터가 잡힐 때가 있지?"의 답은 *fallback resolver*가 단순 타입을 기본으로 잡아주기 때문.

#### (b) Validation 경로

`@Valid` / `@Validated`가 붙은 파라미터 → `LocalValidatorFactoryBean`이 Jakarta Bean Validation 실행. 실패 시:

- `@RequestBody @Valid` → `MethodArgumentNotValidException`
- `@ModelAttribute @Valid` → `BindException`
- 둘 다 `HandlerExceptionResolver`에서 잡아 400 + `ProblemDetail`로 변환됨 (Spring 6+)

#### (d) ReturnValueHandler — HttpMessageConverter 작동 시점

```kotlin
@GetMapping("/users/{id}")
fun get(@PathVariable id: Long): User = userService.find(id)
//                                        ↑
//   반환되는 순간:
//   RequestResponseBodyMethodProcessor가 잡고
//     → 요청의 Accept 헤더 본 다음
//     → 적합한 HttpMessageConverter 선택 (보통 MappingJackson2HttpMessageConverter)
//     → user 객체를 JSON으로 직렬화해서 response.outputStream에 write
```

여기서 응답 body가 *이미 응답 스트림에 쓰여집니다*. → 단계 5(View 렌더링)는 REST에서 사실상 skip.

> 자세한 컨버터 카탈로그와 Content Negotiation 동작은 [`mvc-request-flow.md`](./mvc-request-flow.md) §HttpMessageConverter 참조.

### 2.4 단계 4 — `HandlerInterceptor#postHandle`

- 컨트롤러가 *예외 없이* 반환된 후, View 렌더링 *직전*에 호출.
- 등록 순서의 **역순**으로 호출 (스택 LIFO).
- `ModelAndView`를 받기 때문에 모델에 공통 속성을 추가하는 식의 작업 가능.
- **REST API에서는 거의 가치 없음** — `@ResponseBody`로 이미 응답이 쓰여진 후라 ModelAndView가 비어있음.
- **컨트롤러가 예외를 던지면 호출되지 않음** → 정리 로직은 절대 여기에 두지 말 것.

### 2.5 단계 5 — View 렌더링

- `ModelAndView`에 view 이름이 있을 때만 동작.
- `ViewResolver` 체인이 view 이름을 실제 `View` 객체로 변환 (예: `"home"` → `home.html` Thymeleaf 템플릿).
- `View#render(model, request, response)` → 최종 HTML이 응답에 쓰여짐.
- **`@RestController`는 이 단계 자체를 건너뜀** (이미 단계 3-d에서 body가 쓰여졌고 ModelAndView가 null).

### 2.6 단계 6 — `HandlerInterceptor#afterCompletion`

- View 렌더링까지 *완전히 끝난 뒤* 호출.
- **예외가 발생했어도 호출됨** — `ex` 파라미터에 예외가 담겨 옴.
- 등록 순서의 **역순**.
- "어떤 경우에도 호출됨이 보장"되는 유일한 훅 → **자원 정리/MDC 정리**에 적합.

```kotlin
override fun afterCompletion(req, res, handler, ex: Exception?) {
    val start = req.getAttribute(START_TIME) as? Long ?: return
    val elapsedMs = (System.nanoTime() - start) / 1_000_000
    log.info("handler {} elapsed={}ms ex={}",
        (handler as? HandlerMethod)?.method?.name, elapsedMs, ex?.javaClass?.simpleName)
}
```

### 2.7 예외 경로 — `HandlerExceptionResolver`

컨트롤러 메서드 또는 ArgumentResolver/Validation에서 예외가 나면:

```
컨트롤러 throw Ex
    │
    ▼
HandlerExceptionResolver 체인이 순서대로 시도:
    ① ExceptionHandlerExceptionResolver
       → @ControllerAdvice 또는 같은 컨트롤러의 @ExceptionHandler 메서드 찾아 호출
       → 반환값은 다시 단계 3-d처럼 ReturnValueHandler가 처리(=정상 응답으로 변환)
    ② ResponseStatusExceptionResolver
       → @ResponseStatus(예: HttpStatus.NOT_FOUND) 보고 응답 코드 설정
    ③ DefaultHandlerExceptionResolver
       → Spring이 던지는 표준 예외들(HttpMessageNotReadable 등)을 기본 매핑
    ④ 그래도 못 잡았으면 → 그대로 위로 throw
                       → Servlet Container의 ErrorPage 메커니즘
                       → 일반 500 응답
```

**중요한 함정**:
- `@ControllerAdvice`로 변환된 응답은 **Interceptor의 `afterCompletion(ex)`에 `ex=null`로 들어옴** (이미 정상 응답으로 바뀌었기 때문)
- 원 예외를 추적하려면 Filter의 `try/finally`나 `HandlerExceptionResolver`를 직접 구현하거나, `MDC` 같은 별도 채널에 기록해야 함

### 2.8 단계별 시간 라인 한 장 요약

```
[A] Filter "before"     │ DispatcherServlet 진입 전. 본문 안 읽힘
                        │
   ─────────────────────┼──────────────────────────────────────────────
                        │
   1. HandlerMapping    │ HandlerExecutionChain 생성
   2. preHandle         │ 핸들러 정보 보고 사전 검증
   3. HandlerAdapter    │
      a. ArgumentResolver│ 파라미터 만들기 (여기서 body가 read됨)
      b. Validation     │ @Valid → 실패 시 → ⑦ 예외 경로
      c. invoke()       │ 진짜 컨트롤러 메서드 호출
      d. ReturnValueHandler│ @ResponseBody면 여기서 응답 body가 쓰여짐
   4. postHandle        │ 정상일 때만. ModelAndView 수정 가능
   5. View 렌더링        │ REST면 skip
   6. afterCompletion   │ 예외든 정상이든 무조건 호출. 정리 로직 자리
                        │
   ⑦ 예외 발생 시         │ HandlerExceptionResolver → @ControllerAdvice
                        │
   ─────────────────────┼──────────────────────────────────────────────
                        │
[B] Filter "after"      │ 응답 status/본문 길이 알 수 있음. 로깅 마무리
```

---

## Part 3. 흐름 전체에 걸친 결정 기준

| 하고 싶은 일 | 적절한 자리 | 이유 |
|---|---|---|
| 모든 요청 로깅 (404 포함) | **Filter** | DispatcherServlet 바깥이라 매핑 실패한 요청도 본다 |
| CORS preflight 처리 | **Filter** | 컨트롤러까지 갈 필요 없이 즉시 응답 |
| GZip 압축, 본문 캐싱 | **Filter** | OutputStream Wrapper 교체 필요 |
| 인코딩 강제 | **Filter** | 본문 파싱 *전*에 설정해야 함 |
| 인증/인가 (전역) | **Filter** (Spring Security) | 컨트롤러 도달 전에 차단 + 정적 리소스 포함 |
| 핸들러 메서드 단위 metric | **Interceptor** | `HandlerMethod`를 알아야 함 |
| 메서드 어노테이션 기반 권한 검사 | **Interceptor** | 같은 이유 |
| 컨트롤러 단위 공통 모델 주입 | **Interceptor.postHandle** | `ModelAndView`를 받음 |
| 파라미터 바인딩 커스터마이즈 | **`HandlerMethodArgumentResolver` 추가** | DispatcherServlet 내부 확장점 |
| 반환값 처리 커스터마이즈 | **`HandlerMethodReturnValueHandler` 추가** | 같은 이유 |
| 전역 예외 → 응답 변환 | **`@ControllerAdvice` + `@ExceptionHandler`** | `HandlerExceptionResolver` 체인이 자동 매핑 |

---

## Part 4. 자주 빠지는 함정 (전체 흐름 관점)

1. **Interceptor에서 응답 body 가공을 시도** — `@ResponseBody`로 이미 응답이 쓰여진 후라 불가능. body 가공은 Filter에서 `ContentCachingResponseWrapper`로.
2. **`@ControllerAdvice`로 잡힌 예외를 Interceptor `afterCompletion(ex)`로 추적** — Spring이 정상 응답으로 바꿔서 `ex=null`로 들어옴. 원 예외가 필요하면 Filter 또는 직접 `HandlerExceptionResolver` 구현.
3. **`postHandle`에 자원 정리 로직** — 예외 시 호출 안 됨. 정리는 항상 `afterCompletion`.
4. **인증을 Interceptor로 구현** — 동작은 하지만 Spring Security와 책임이 겹치고 정적 리소스 미적용 등 빈틈 발생.
5. **컨트롤러에서 `HttpServletResponse#setStatus()`를 직접 호출** — 이후 ReturnValueHandler가 덮어쓸 수 있음. `ResponseEntity` 또는 `@ResponseStatus`를 사용.
6. **`@RequestParam` 없이 단순 타입 파라미터를 받았는데 다른 모듈에선 안 됨** — 모듈별 ArgumentResolver 설정 차이. 명시적으로 어노테이션 붙이는 게 안전.
7. **`OncePerRequestFilter` 안 쓰고 단순 Filter로 등록** — `RequestDispatcher.forward`/`include` 시 중복 호출돼 로그가 두 번 찍힘.

---

## Part 5. 관련 코드

| 파일 | 역할 |
|---|---|
| [`web/filter/RequestLoggingFilter.kt`](../src/main/kotlin/me/victor/spring01/web/filter/RequestLoggingFilter.kt) | Filter의 `try/finally`로 [A]+[B] 시점 모두 활용 |
| [`web/interceptor/HandlerTimingInterceptor.kt`](../src/main/kotlin/me/victor/spring01/web/interceptor/HandlerTimingInterceptor.kt) | `HandlerMethod`로 핸들러 단위 metric. `afterCompletion`에 정리 |
| [`web/WebConfig.kt`](../src/main/kotlin/me/victor/spring01/web/WebConfig.kt) | Filter는 `FilterRegistrationBean`, Interceptor는 `WebMvcConfigurer`로 등록 |
| [`learning/mvc/EchoController.kt`](../src/main/kotlin/me/victor/spring01/learning/mvc/EchoController.kt) | ArgumentResolver/ReturnValueHandler가 다루는 모든 어노테이션 케이스 |

---

## 한 문장 요약

> Filter는 **Servlet Container 안, DispatcherServlet 밖**에서 요청/응답 *전체*를 감싼다.
> DispatcherServlet은 들어온 요청을 **HandlerMapping → HandlerAdapter(ArgumentResolver/Validation/Invoke/ReturnValueHandler) → Interceptor 훅 → View/Exception**의 7단계로 처리하는 Front Controller다.
> "어디서 끼어들 수 있는가"는 "그 시점에 무엇이 이미 결정됐는가"로 결정된다.
