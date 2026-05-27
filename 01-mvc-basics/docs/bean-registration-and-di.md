# Bean 등록과 의존성 주입 (DI)

> Tier 1 #1 — IoC / DI / Bean 라이프사이클의 **첫 번째 조각**.
> Scope / 라이프사이클은 [`bean-scope-and-lifecycle.md`](./bean-scope-and-lifecycle.md), `@ConfigurationProperties`는 [`configuration-properties.md`](./configuration-properties.md)에서 다룬다.

Spring의 본질은 두 가지로 요약된다:
1. **어떤 객체를 컨테이너가 관리하게 만들 것인가** (Bean 등록)
2. **그 객체들을 서로 어떻게 연결할 것인가** (DI)

---

## IoC가 뭐고 왜 필요한가

전통 OOP에서는 객체가 직접 의존성을 만든다:

```kotlin
class GreetingService {
    private val greeter = KoreanGreeter() // ← 새로 만든다
}
```

이러면 생기는 문제:
- 테스트할 때 `greeter`를 다른 구현체로 갈아끼우기 어렵다 (mock 주입 불가).
- 환경별로 다른 구현체(예: dev면 가짜, prod면 진짜)를 쓰기 까다롭다.
- 의존성 그래프가 코드 곳곳에 박혀 있어 변경 영향이 크다.

**IoC(Inversion of Control)** — "객체가 의존성을 직접 만들지 않고, 외부에서 받는다." Spring `ApplicationContext`가 의존성 그래프를 가지고 있다가, 누가 누구를 필요로 하는지 보고 만들고 주입(inject)한다.

---

## Bean 등록 방법

| 방법 | 언제 쓰나 |
| --- | --- |
| `@Component` (+ `@Service`/`@Repository`/`@Controller`) | **내가 만든 클래스**. 컴포넌트 스캔이 자동으로 발견. 가장 흔함. |
| `@Bean` 메서드 in `@Configuration` 클래스 | **내가 만들지 않은 클래스** (예: `Clock`, `ObjectMapper`, `DataSource`) 또는 인스턴스 생성 로직에 조건 분기가 필요할 때. |
| `GenericApplicationContext.registerBean` (프로그래밍 방식) | 거의 안 씀. 동적 등록이 정말 필요한 경우. |

`@Service`/`@Repository`/`@Controller`는 모두 `@Component`의 변형으로, **기능적 차이는 거의 없고 의도만 다르다**. `@Repository`는 추가로 DataAccessException 변환 같은 옵션이 있긴 함.

### 결정 트리

```
Q. Bean으로 만들 클래스를 내가 직접 작성했는가?
   ├─ YES → @Component (의도에 따라 @Service / @Repository / @Controller)
   └─ NO  → @Configuration + @Bean
```

---

## DI 방식 3가지

### 1. Constructor Injection (✅ 권장)

```kotlin
@Service
class GreetingService(
    private val greeter: Greeter,
    private val clock: Clock,
)
```

- `val` 불변. 객체가 항상 완전한 상태로 존재.
- 의존성이 시그니처에 노출 → 한눈에 "이 클래스가 뭘 필요로 하는지" 보임.
- 테스트 시 mock을 그냥 생성자로 넘기면 됨.
- 순환 의존성이 기동 시점에 강제로 드러남.
- Spring 4.3+ 부터 **단일 생성자면 `@Autowired` 생략 가능**.

### 2. Setter Injection

```kotlin
@Service
class GreetingService {
    private lateinit var greeter: Greeter

    @Autowired
    fun setGreeter(greeter: Greeter) { this.greeter = greeter }
}
```

- 선택적 의존성을 표현할 때 가끔 사용.
- 단점: 객체가 일시적으로 "불완전 상태"로 존재할 수 있고, 누가 setter를 우회로 호출할 위험.

### 3. Field Injection (❌ 안티 패턴)

```kotlin
@Service
class GreetingService {
    @Autowired
    private lateinit var greeter: Greeter // ← 권장하지 않음
}
```

옛 한국어 자료에서 매우 흔히 보이지만, 현대 Spring 컨벤션에서는 비권장:
- 테스트 시 mock 주입에 reflection 필요.
- 의존성이 클래스 본문 안에 "숨어" 있어 가독성이 떨어짐.
- `val` (final) 불가.
- 순환 의존성을 우회해버려 진짜 문제를 늦게 발견.

Kotlin에서는 constructor 주입이 가장 자연스럽다 (`class Service(val greeter: Greeter)`).

---

## 다수 후보가 있을 때 — `@Primary` vs `@Qualifier`

같은 타입 Bean이 여러 개 등록되면 Spring은 어느 걸 주입할지 모름 → `NoUniqueBeanDefinitionException`. 해결책 둘:

### `@Primary` — 기본값을 하나 정한다

```kotlin
@Component
@Primary
class KoreanGreeter : Greeter { ... }
```

다른 모든 곳에서 `Greeter` 주입 받으면 `KoreanGreeter`가 들어온다. 부르는 쪽 코드가 깔끔해지지만, **남발하면 의존성이 어디서 오는지 흐려진다**. "정말로 기본이라 부를 만한 후보"에만 사용.

### `@Qualifier` — 부르는 쪽에서 콕 집는다

```kotlin
@Component
@Qualifier("japanese")
class JapaneseGreeter : Greeter { ... }

@Service
class QualifiedGreetingService(
    @Qualifier("japanese") private val greeter: Greeter,
)
```

명시적이고 안전하지만, 부르는 쪽 코드가 늘어남.

### 어떤 걸 언제?

```
Q. 후보 중 "기본"이라 부를 만한 게 명확한가?
   ├─ YES → @Primary로 기본 지정, 나머지는 @Qualifier로 콕 집는다.
   └─ NO  → @Primary 없이 모두 @Qualifier로 명시적 매칭.
```

---

## 다이어그램

```
┌──────────────────────────────────────────────────────┐
│  ApplicationContext (IoC Container)                  │
│                                                      │
│   ┌────────────────────┐                             │
│   │  EnglishGreeter    │                             │
│   └────────────────────┘                             │
│   ┌────────────────────┐ ← @Primary                  │
│   │  KoreanGreeter     │  ─── "Greeter" 기본 후보   │
│   └─────────┬──────────┘                             │
│             │  (생성자 주입, 별도 표시 없으면 이게)  │
│             ▼                                        │
│   ┌────────────────────┐    ┌────────────────────┐   │
│   │  GreetingService   │ ◀──│  systemClock       │   │
│   └────────────────────┘    │  (@Bean in         │   │
│                             │   ClockConfig)     │   │
│                             └────────────────────┘   │
│   ┌────────────────────┐ ← @Qualifier("japanese")   │
│   │  JapaneseGreeter   │                             │
│   └─────────┬──────────┘                             │
│             │  @Qualifier("japanese")                │
│             ▼                                        │
│   ┌──────────────────────────┐                       │
│   │ QualifiedGreetingService │                       │
│   └──────────────────────────┘                       │
│                                                      │
│   ┌──────────────────────────┐                       │
│   │ iocDemoRunner            │  (ApplicationRunner) │
│   │  ↳ 시작 시 두 서비스 호출 │                      │
│   └──────────────────────────┘                       │
└──────────────────────────────────────────────────────┘
```

---

## 이 모듈의 예제 코드

| 파일 | 역할 |
| --- | --- |
| [`learning/ioc/Greeters.kt`](../src/main/kotlin/me/victor/spring01/learning/ioc/Greeters.kt) | `Greeter` 인터페이스 + 3개 구현체. `@Primary` / `@Qualifier`. |
| [`learning/ioc/ClockConfig.kt`](../src/main/kotlin/me/victor/spring01/learning/ioc/ClockConfig.kt) | `@Configuration` + `@Bean` 스타일 (서드파티 `Clock` 등록). |
| [`learning/ioc/GreetingService.kt`](../src/main/kotlin/me/victor/spring01/learning/ioc/GreetingService.kt) | Constructor 주입, `@Primary` 후보를 받음. |
| [`learning/ioc/QualifiedGreetingService.kt`](../src/main/kotlin/me/victor/spring01/learning/ioc/QualifiedGreetingService.kt) | `@Qualifier("japanese")`로 특정 후보 주입. |
| [`learning/ioc/IocDemoConfig.kt`](../src/main/kotlin/me/victor/spring01/learning/ioc/IocDemoConfig.kt) | `ApplicationRunner`로 시작 시 두 서비스 호출 결과 출력. |

### 실행해서 확인

```bash
./gradlew :01-mvc-basics:bootRun
```

기동 로그 후반부에 다음 두 줄이 보여야 한다:

```
IocDemoRunner : primary greeting   = [HH:mm:ss] 안녕하세요, victor 님!
IocDemoRunner : qualified greeting = こんにちは、victor さん!
```

---

## 자주 빠지는 함정

1. **Field injection을 계속 쓴다** — 옛 자료의 영향. 현대 Spring에선 constructor 주입이 정답.
2. **단일 생성자에도 `@Autowired`를 붙인다** — Spring 4.3+ 이후 생략 가능. 붙여도 동작은 하지만 노이즈.
3. **`@Primary`를 남발한다** — 두 개에 동시에 붙이면 또 충돌. "정말 기본"인 후보에만.
4. **컴포넌트 스캔 범위를 벗어난 곳에 `@Component`를 둔다** — `@SpringBootApplication`이 붙은 클래스의 패키지(및 하위)만 자동 스캔. 형제 패키지에 두면 발견 안 됨. 이 모듈의 경우 `me.victor.spring01` 하위면 OK.
5. **순환 의존성을 `@Lazy`로 우회한다** — 동작은 하지만 설계 결함의 신호. 근본 해결은 책임 분리.
6. **같은 타입 후보가 여러 개인데 아무 표시도 없다** — `NoUniqueBeanDefinitionException`. `@Primary` 또는 `@Qualifier`로 해결.

---

## 다음

- **Scope, 라이프사이클(`@PostConstruct`, `BeanPostProcessor`)** → [`bean-scope-and-lifecycle.md`](./bean-scope-and-lifecycle.md)
- **`@ConfigurationProperties` + Kotlin data class** → [`configuration-properties.md`](./configuration-properties.md)
