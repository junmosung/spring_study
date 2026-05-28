# Bean Scope와 라이프사이클

> Tier 1 #1 — IoC / DI / Bean 라이프사이클의 **두 번째 조각**.
> 등록·DI는 [`bean-registration-and-di.md`](./bean-registration-and-di.md), `@ConfigurationProperties`는 [`configuration-properties.md`](./configuration-properties.md)에서 다룬다.

Bean이 **몇 개 존재할 것인가**(Scope), 그리고 **언제 만들어지고 언제 사라지는가**(라이프사이클)에 대한 이야기.

---

## Scope 종류

| Scope | 인스턴스 개수 | 언제 쓰나 |
| --- | --- | --- |
| **singleton** (기본) | 컨테이너당 1개 | 99%의 stateless 서비스. 가장 흔함. |
| **prototype** | 요청할 때마다 새로 | 호출 단위로 상태가 다른 경량 객체. 흔치 않음. |
| **request** | HTTP 요청당 1개 | 요청 단위 컨텍스트 (요청 시작 ID, 사용자 정보 캐시 등). |
| **session** | HTTP 세션당 1개 | 사용자 세션 단위 데이터. SPA·세션리스 API에선 거의 불필요. |
| **application** | ServletContext 단위 1개 | 사실상 singleton과 거의 동일. 거의 안 씀. |
| **websocket** | WebSocket 세션당 1개 | WebSocket 통신할 때만. |

> singleton이 적합하지 않다는 강한 이유가 없으면 그대로 둔다. 상태가 있는 객체에 무의식적으로 singleton을 두면 동시성 버그의 출발점이 된다.

### Prototype을 singleton에 주입하는 함정

```kotlin
// ❌ 이렇게 직접 주입하면 매번 새 인스턴스가 아님
@Service
class Issuer(private val ticket: PrototypeTicket) {
    fun issue() = ticket   // 항상 같은 인스턴스를 돌려준다!
}
```

`Issuer`가 생성될 때 한 번 `PrototypeTicket` 인스턴스를 받아 박는다. 그 뒤로는 같은 객체를 본다. Prototype의 의미가 사라진다.

해결: **`ObjectProvider<T>`**로 호출 시점에 컨테이너에 요청.

```kotlin
@Service
class Issuer(private val tickets: ObjectProvider<PrototypeTicket>) {
    fun issue(): PrototypeTicket = tickets.getObject() // ← 매번 새로
}
```

대안: `jakarta.inject.Provider<T>` (CDI 표준, 동일 동작), `@Lookup` 메서드, `ApplicationContext.getBean(...)` 직접 호출(비권장).

---

## 라이프사이클 흐름

```
                     기동                                            종료
                      │                                              │
                      ▼                                              ▼
              ┌───────────────┐                            ┌───────────────┐
              │  1. 생성자    │                            │ D1. @PreDestroy│
              └──────┬────────┘                            └──────┬────────┘
                     ▼                                            ▼
              ┌───────────────┐                            ┌───────────────┐
              │  의존성 주입   │                            │ D2. Disposable │
              │  (DI)         │                            │     #destroy() │
              └──────┬────────┘                            └───────────────┘
                     ▼
              ┌───────────────┐
              │ 2. *Aware      │   (BeanNameAware, ApplicationContextAware 등)
              │   #set...     │
              └──────┬────────┘
                     ▼
              ┌──────────────────────────┐
              │ 3. BeanPostProcessor      │
              │    #postProcessBefore...  │
              └──────┬───────────────────┘
                     ▼
              ┌───────────────┐
              │ 4. @PostConstruct │  ◀── 보통 여기서 초기화 로직
              └──────┬────────┘
                     ▼
              ┌───────────────────────────┐
              │ 5. InitializingBean        │
              │    #afterPropertiesSet     │
              └──────┬────────────────────┘
                     ▼
              ┌──────────────────────────┐
              │ 6. BeanPostProcessor      │  ◀── AOP/@Transactional 프록시가 끼어드는 곳
              │    #postProcessAfter...   │
              └──────┬───────────────────┘
                     ▼
              ┌───────────────┐
              │   Ready       │
              └───────────────┘
```

### 라이프사이클 훅 3가지

| 방법 | 권장도 | 비고 |
| --- | --- | --- |
| `@PostConstruct` / `@PreDestroy` | ✅ 권장 | jakarta 표준 어노테이션. Spring 의존성을 클래스에 박지 않음. |
| `InitializingBean` / `DisposableBean` | ⚠️ 비추 | Spring 인터페이스. 클래스가 Spring에 강결합. |
| `@Bean(initMethod=..., destroyMethod=...)` | 가끔 | 우리가 만들지 않은 클래스(서드파티)에 훅을 걸어야 할 때 유용. |

---

## BeanPostProcessor

컨테이너의 **모든 빈** 초기화 직전·직후에 호출되는 후크. Spring의 AOP·`@Transactional`·`@Async` 등 거의 모든 마법이 BeanPostProcessor를 통해 빈을 프록시로 교체하는 방식으로 구현돼 있다.

```kotlin
@Component
class MyBpp : BeanPostProcessor {
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        // 빈을 그대로 반환하거나, 다른 객체(프록시 등)로 교체해 반환할 수 있다.
        return bean
    }
}
```

**주의**: BeanPostProcessor 자신은 일반 빈보다 먼저 만들어진다. 그래서 BPP 안에 `@Autowired`로 다른 비즈니스 빈을 받으려 하면, 그 빈은 BPP가 처리해주기 전에 일찍 생성돼 BPP 적용을 못 받는 위치에 놓일 수 있다. 가능하면 BPP는 의존성 없이 두는 게 안전.

---

## 이 모듈의 예제 코드

| 파일 | 역할 |
| --- | --- |
| [`learning/lifecycle/SingletonAndPrototype.kt`](../src/main/kotlin/me/victor/spring01/learning/lifecycle/SingletonAndPrototype.kt) | `SingletonCounter` (기본), `PrototypeTicket` (`@Scope`) |
| [`learning/lifecycle/TicketIssuer.kt`](../src/main/kotlin/me/victor/spring01/learning/lifecycle/TicketIssuer.kt) | `ObjectProvider<T>`로 prototype 함정 회피 |
| [`learning/lifecycle/LifecycleAwareBean.kt`](../src/main/kotlin/me/victor/spring01/learning/lifecycle/LifecycleAwareBean.kt) | 모든 훅(`@PostConstruct`, `InitializingBean`, `BeanNameAware`, `@PreDestroy`, `DisposableBean`) 한 번씩 |
| [`learning/lifecycle/LoggingBeanPostProcessor.kt`](../src/main/kotlin/me/victor/spring01/learning/lifecycle/LoggingBeanPostProcessor.kt) | 모든 빈에 적용되는 BPP, learning 패키지만 로그 |
| [`learning/lifecycle/LifecycleDemoConfig.kt`](../src/main/kotlin/me/victor/spring01/learning/lifecycle/LifecycleDemoConfig.kt) | singleton 동일성·prototype 차이 검증 |

### 실행해서 확인

```bash
./gradlew :01-mvc-basics:bootRun
```

기동 로그에서 다음 순서로 LifecycleAwareBean 훅이 호출돼야 한다 (`[lifecycle] N.` 접두):

```
1. constructor
2. BeanNameAware#setBeanName = lifecycleAwareBean
3. BPP#before  lifecycleAwareBean
4. @PostConstruct (lifecycleAwareBean)
5. InitializingBean#afterPropertiesSet (lifecycleAwareBean)
6. BPP#after   lifecycleAwareBean
```

그리고 `LifecycleDemoRunner`가 다음과 같이 출력:

```
singleton same instance? true
singleton counter = 1, 2, 3
prototype ids = 1, 2  (different instance? true)
```

`Ctrl+C` 로 graceful shutdown 시 종료 훅도 보인다:

```
D1. @PreDestroy (lifecycleAwareBean)
D2. DisposableBean#destroy (lifecycleAwareBean)
```

> `kill -9` (SIGKILL)은 JVM 종료를 차단할 기회를 주지 않으므로 destroy 훅이 호출되지 않는다.

---

## 자주 빠지는 함정

1. **Prototype을 singleton에 그냥 주입한다** — 매번 새 인스턴스가 아니다. `ObjectProvider<T>` 사용.
2. **상태 있는 객체에 무의식적으로 singleton을 둔다** — 동시성 버그의 출발점. 일단 ThreadLocal 또는 method-local 상태로 두고, 진짜 빈이어야 하는지 다시 검토.
3. **`@PostConstruct`에서 무거운 초기화** — 기동이 길어지고 실패 원인 추적이 어려워진다. 진짜 무거운 초기화는 `ApplicationRunner` 또는 별도 시작 시점 작업으로 분리.
4. **destroy 훅에 의존한 청소** — `kill -9`, OOM kill, JVM crash 시 호출되지 않는다. "꼭 떨어져야 하는" 정리는 컨테이너 바깥(외부 리소스 lease, DB 트랜잭션 등)으로.
5. **BeanPostProcessor에 비즈니스 빈을 `@Autowired`** — BPP가 너무 일찍 만들어지기 때문에 의존성 빈은 BPP 처리를 못 받는 위치에 놓일 수 있다. 정 필요하면 `ApplicationContextAware` + 지연 조회로 우회.
6. **`@PostConstruct`가 final/static/private 메서드에 없다** — JDK 동적 프록시·CGLIB와의 상호작용에서 호출 안 될 수 있음. public 인스턴스 메서드에 붙일 것.

---

## 다음

- **`@ConfigurationProperties` + Kotlin data class** → [`configuration-properties.md`](./configuration-properties.md)
