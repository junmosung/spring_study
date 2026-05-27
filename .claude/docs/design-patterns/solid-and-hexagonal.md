# SOLID와 헥사고날, 비판적으로 다시 보기

> "맹신할 필요 없는 유행"이라는 직관에서 출발한 정리.
> 결론: **원칙을 외우지 말고, 그 원칙이 해결하려던 *문제*를 외우자.**

---

## 0. 큰 그림

| 주제 | 핵심 약속 | 자주 빠지는 함정 |
|---|---|---|
| SOLID | 변경에 강한 클래스/모듈 설계 원칙 5가지 | 체크리스트화 → 과설계, indirection 지옥 |
| Hexagonal | 도메인을 인프라(DB/HTTP/UI)로부터 격리 | 폴더만 헥사고날, 도메인은 여전히 JPA 범벅 |

두 패턴 모두 **실제 고통에서 나온 처방**이지만,
그 고통을 겪지 않은 상태에서 처방만 따라 하면 **카고 컬트(cargo cult)** 가 됩니다.

---

## 1. SOLID — 원칙별 비판적 정리

### 1.1 S — Single Responsibility Principle

> "한 클래스는 *하나의 액터(stakeholder)* 에 대해서만 변경 이유를 가져야 한다."
> — Robert C. Martin (정의가 여러 번 바뀐 점에 주의)

#### 나쁜 예 — 진짜로 두 액터의 책임이 섞임

```kotlin
class Employee(
    val id: Long,
    val name: String,
    val hoursWorked: Int,
    val hourlyRate: Int,
) {
    // 회계팀의 요구로 바뀜
    fun calculatePay(): Int = hoursWorked * hourlyRate

    // 인사팀의 요구로 바뀜 (근무시간 정책)
    fun reportHours(): String = "근무시간: $hoursWorked h"

    // DBA 팀의 요구로 바뀜
    fun save(connection: java.sql.Connection) { /* ... */ }
}
```

세 부서가 같은 클래스를 건드리면 **머지 충돌 + 의도치 않은 영향**이 생깁니다.

#### 좋은 예 — 액터별로 분리

```kotlin
data class Employee(val id: Long, val name: String, val hoursWorked: Int, val hourlyRate: Int)

class PayCalculator       { fun calculate(e: Employee): Int = e.hoursWorked * e.hourlyRate }
class HourReporter        { fun report(e: Employee): String  = "근무시간: ${e.hoursWorked} h" }
class EmployeeRepository  { fun save(e: Employee) { /* ... */ } }
```

#### ⚠️ 함정
- "이 클래스에 메서드가 2개니까 쪼개야 해" — **틀린 적용**. 책임은 메서드 개수가 아니라 **변경을 요청하는 사람**.
- 쪼개기를 과하게 하면 한 줄짜리 클래스 10개로 **응집도가 깨짐**.

---

### 1.2 O — Open/Closed Principle

> "확장에는 열려있고, 수정에는 닫혀있어야 한다."

#### 나쁜 예 — 새 결제 수단마다 if/else를 늘림

```kotlin
class PaymentProcessor {
    fun process(type: String, amount: Int) {
        when (type) {
            "CARD"   -> { /* 카드 결제 */ }
            "BANK"   -> { /* 계좌이체 */ }
            "KAKAO"  -> { /* 카카오페이 */ }
            // 새 결제 수단마다 이 파일을 수정해야 함
        }
    }
}
```

#### 좋은 예 — 전략(Strategy)으로 확장점만 열기

```kotlin
interface PaymentMethod { fun pay(amount: Int) }

class CardPayment  : PaymentMethod { override fun pay(amount: Int) { /* ... */ } }
class BankPayment  : PaymentMethod { override fun pay(amount: Int) { /* ... */ } }
class KakaoPayment : PaymentMethod { override fun pay(amount: Int) { /* ... */ } }

class PaymentProcessor(private val method: PaymentMethod) {
    fun process(amount: Int) = method.pay(amount)
}
```

#### ⚠️ 함정
- **YAGNI 위반의 단골**. "혹시 나중에..." 라며 미리 만든 확장점은 거의 항상 틀린 모양으로 굳습니다.
- **Rule of Three**: 같은 종류의 분기를 **세 번** 마주칠 때까지는 그냥 if/else로 두세요.
- 라이브러리/프레임워크처럼 *외부에 노출되는 API*에서는 OCP가 유효, 애플리케이션 내부에서는 신중하게.

---

### 1.3 L — Liskov Substitution Principle

> "S가 T의 하위 타입이면, T를 쓰는 어디서든 S로 바꿔도 프로그램이 깨지면 안 된다."

#### 나쁜 예 — 정사각형/사각형 고전 문제

```kotlin
open class Rectangle(open var width: Int, open var height: Int) {
    open fun area(): Int = width * height
}

class Square(side: Int) : Rectangle(side, side) {
    override var width: Int
        get() = super.width
        set(value) { super.width = value; super.height = value } // 부수효과!
    override var height: Int
        get() = super.height
        set(value) { super.width = value; super.height = value }
}

fun resize(r: Rectangle) {
    r.width = 5
    r.height = 4
    check(r.area() == 20) // Square를 넣으면 16이 나와서 깨짐 ❌
}
```

#### 좋은 예 — 상속 대신 별도 타입

```kotlin
sealed interface Shape { fun area(): Int }
data class Rectangle(val width: Int, val height: Int) : Shape { override fun area() = width * height }
data class Square(val side: Int) : Shape { override fun area() = side * side }
```

#### ⚠️ 함정 — 사실 LSP는 가장 단단한 원칙
- 5개 중 **유일하게 타입 이론적 근거가 있는 원칙**. 유행이 아닙니다.
- 다만 요즘은 **상속 자체를 잘 안 쓰는 것**이 더 건강한 방향 → composition over inheritance.

---

### 1.4 I — Interface Segregation Principle

> "클라이언트가 자신이 사용하지 않는 메서드에 의존하도록 강요받아서는 안 된다."

#### 나쁜 예 — 한 인터페이스에 모든 능력을 욱여넣음

```kotlin
interface Worker {
    fun work()
    fun eat()      // 로봇은 안 먹음
    fun sleep()    // 로봇은 안 잠
}

class Robot : Worker {
    override fun work() { /* OK */ }
    override fun eat()   { throw UnsupportedOperationException() }   // ❌
    override fun sleep() { throw UnsupportedOperationException() }   // ❌
}
```

#### 좋은 예 — 능력별로 분리

```kotlin
interface Workable { fun work() }
interface Feedable { fun eat() }
interface Restable { fun sleep() }

class Human : Workable, Feedable, Restable { /* ... */ }
class Robot : Workable                     { override fun work() { /* ... */ } }
```

#### ⚠️ 함정
- 극단으로 가면 **메서드 1개짜리 인터페이스 폭발**.
- Kotlin에서는 `interface` 대신 **함수 타입**(`(Int) -> Boolean` 등)으로 충분한 경우가 많음.

---

### 1.5 D — Dependency Inversion Principle

> "상위 모듈은 하위 모듈에 의존하지 않는다. 둘 다 추상에 의존해야 한다."

#### 나쁜 예 — 서비스가 구체 DB 클래스에 직접 의존

```kotlin
class MySqlUserRepository {
    fun findById(id: Long): User { /* MySQL 쿼리 */ TODO() }
}

class UserService {
    private val repo = MySqlUserRepository() // ❌ 구체 클래스에 못 박힘
    fun get(id: Long) = repo.findById(id)
}
```

#### 좋은 예 — 추상에 의존, 구현은 주입

```kotlin
interface UserRepository { fun findById(id: Long): User }

class MySqlUserRepository : UserRepository {
    override fun findById(id: Long): User { TODO() }
}

class UserService(private val repo: UserRepository) { // ✅ 추상에 의존
    fun get(id: Long) = repo.findById(id)
}
```

#### ⚠️ 함정
- **구현이 1개뿐인 인터페이스(`UserService` ↔ `UserServiceImpl`)는 거의 노이즈**. 테스트 더블이나 다중 구현 가능성이 *실제로* 보일 때 도입.
- Kotlin에서는 인터페이스 대신 **함수 타입 주입**으로도 충분: `class UserService(val findById: (Long) -> User)`.

---

## 2. Hexagonal Architecture (Ports & Adapters)

### 2.1 핵심 다이어그램

```
                         ┌─────────────────────────────┐
                         │       Driving Adapters      │
                         │   (HTTP, CLI, 메시지 컨슈머)  │
                         └──────────────┬──────────────┘
                                        │ (Driving Port = UseCase 인터페이스 호출)
                                        ▼
        ┌──────────────────────────────────────────────────────────────┐
        │                       Application Core                       │
        │                                                              │
        │   ┌────────────────┐    ┌──────────────────────────────┐    │
        │   │  Driving Ports │    │      Domain Model            │    │
        │   │  (UseCases)    │───▶│  (Entities, Value Objects,   │    │
        │   │                │    │   Domain Services, Events)   │    │
        │   └────────────────┘    └──────────────────────────────┘    │
        │                                  │                          │
        │                                  ▼                          │
        │                        ┌──────────────────┐                 │
        │                        │  Driven Ports    │                 │
        │                        │  (Repository,    │                 │
        │                        │   Notifier, ...) │                 │
        │                        └────────┬─────────┘                 │
        └─────────────────────────────────┼────────────────────────────┘
                                          │ (인터페이스 구현)
                                          ▼
                         ┌─────────────────────────────┐
                         │       Driven Adapters       │
                         │   (JPA, Kafka, SMTP, S3)    │
                         └─────────────────────────────┘

   바깥쪽(어댑터) ──► 안쪽(도메인) 으로만 의존 방향이 흐른다.
   도메인은 Spring/JPA/HTTP 어떤 것도 모른다.
```

### 2.2 전통적 Layered와의 차이

```
[Layered]                              [Hexagonal]

Controller                             HttpAdapter ──┐
   │                                                  │
   ▼                                                  ▼
Service           ◀──→  도메인이 JPA를       UseCase (Port)
   │                     알게 되는 경향                │
   ▼                                                  ▼
Repository(JPA)                                    Domain (POJO)
   │                                                  ▲
   ▼                                                  │
   DB                                              RepositoryPort
                                                      ▲
                                                      │
                                                JpaRepositoryAdapter
                                                      │
                                                      ▼
                                                      DB
```

핵심 차이: **도메인이 JPA를 모르는가**.
폴더만 `port/adapter`로 나누고 도메인에 `@Entity`가 붙어 있다면 그건 헥사고날이 아닙니다.

### 2.3 Kotlin 예시 — 주문(Order) 도메인

```kotlin
// ─────────────────────────────────────────────
// (1) 도메인 — 순수 Kotlin, Spring/JPA 의존 0
// ─────────────────────────────────────────────
package com.example.order.domain

data class OrderId(val value: Long)
data class Money(val amount: Long) {
    operator fun plus(other: Money) = Money(amount + other.amount)
}

class Order(
    val id: OrderId,
    val items: List<OrderLine>,
) {
    fun total(): Money = items.fold(Money(0)) { acc, line -> acc + line.subtotal() }
}

data class OrderLine(val productId: Long, val price: Money, val quantity: Int) {
    fun subtotal() = Money(price.amount * quantity)
}

// ─────────────────────────────────────────────
// (2) Driving Port — 바깥에서 도메인을 부르는 입구
// ─────────────────────────────────────────────
package com.example.order.application.port.`in`

interface PlaceOrderUseCase {
    fun place(command: PlaceOrderCommand): OrderId
}
data class PlaceOrderCommand(val items: List<OrderLine>)

// ─────────────────────────────────────────────
// (3) Driven Port — 도메인이 바깥에 요구하는 능력
// ─────────────────────────────────────────────
package com.example.order.application.port.out

interface OrderRepository {
    fun save(order: Order): Order
    fun nextId(): OrderId
}
interface PaymentGateway {
    fun charge(orderId: OrderId, amount: Money)
}

// ─────────────────────────────────────────────
// (4) Application Service — 유스케이스 오케스트레이션
// ─────────────────────────────────────────────
package com.example.order.application

class PlaceOrderService(
    private val orders: OrderRepository,
    private val payments: PaymentGateway,
) : PlaceOrderUseCase {
    override fun place(command: PlaceOrderCommand): OrderId {
        val order = Order(orders.nextId(), command.items)
        val saved = orders.save(order)
        payments.charge(saved.id, saved.total())
        return saved.id
    }
}

// ─────────────────────────────────────────────
// (5) Driven Adapter — JPA로 OrderRepository 구현
// ─────────────────────────────────────────────
package com.example.order.adapter.out.persistence

import jakarta.persistence.*

@Entity @Table(name = "orders")
class OrderJpaEntity(
    @Id @GeneratedValue var id: Long? = null,
    @Column(name = "total") var total: Long = 0,
    // ...
)

class OrderPersistenceAdapter(
    private val jpa: OrderJpaRepository,
) : OrderRepository {
    override fun save(order: Order): Order {
        val entity = OrderJpaEntity(id = order.id.value, total = order.total().amount)
        jpa.save(entity)
        return order
    }
    override fun nextId(): OrderId = OrderId(System.nanoTime()) // 예시
}

// ─────────────────────────────────────────────
// (6) Driving Adapter — Spring MVC 컨트롤러
// ─────────────────────────────────────────────
package com.example.order.adapter.`in`.web

import org.springframework.web.bind.annotation.*

@RestController @RequestMapping("/orders")
class OrderController(private val useCase: PlaceOrderUseCase) {
    @PostMapping
    fun place(@RequestBody req: PlaceOrderRequest): PlaceOrderResponse {
        val id = useCase.place(PlaceOrderCommand(req.toDomain()))
        return PlaceOrderResponse(id.value)
    }
}
```

### 2.4 권장 폴더 구조

```
order/
├── domain/                      ← 순수 Kotlin. Spring 의존 금지.
│   ├── Order.kt
│   ├── OrderLine.kt
│   └── Money.kt
├── application/
│   ├── port/
│   │   ├── in/  PlaceOrderUseCase.kt
│   │   └── out/ OrderRepository.kt, PaymentGateway.kt
│   └── service/ PlaceOrderService.kt
└── adapter/
    ├── in/
    │   ├── web/   OrderController.kt
    │   └── cli/   OrderCli.kt
    └── out/
        ├── persistence/  OrderPersistenceAdapter.kt, OrderJpaEntity.kt
        └── payment/      TossPaymentAdapter.kt
```

의존 규칙: **`adapter` → `application` → `domain`** (한 방향).
도메인이 `import jakarta.persistence.*` 하면 그 순간 헥사고날은 깨진 것.

### 2.5 장단점 솔직 정리

| 측면 | 장점 | 단점 |
|---|---|---|
| 테스트 | 도메인을 인메모리 어댑터로 단위 테스트 가능 | 테스트 더블/어댑터 작성 비용 |
| 변경 영향 | 인프라 교체가 도메인을 안 건드림 | 변환 레이어(DTO ↔ Domain ↔ Entity) 보일러플레이트 |
| 협업 | 도메인이 깨끗 → 비즈니스 규칙 가독성 ↑ | 신규 팀원 진입 비용 ↑ |
| 적합한 곳 | 도메인 복잡도 高 (금융/보험/물류) | CRUD 위주, MVP, 단기 프로젝트 |

---

## 3. 의사결정 가이드

```
                  ┌──────────────────────────────┐
                  │  새 모듈/서비스를 설계 중인가? │
                  └──────────────┬───────────────┘
                                 ▼
                ┌──────────────────────────────────┐
                │ 도메인 규칙이 "조건 + 분기"가 많고  │
                │ CRUD가 아닌가?                   │
                └──────────────┬───────────────────┘
                  Yes          │           No
        ┌────────────────────┐ │ ┌────────────────────────┐
        │ 수명이 3년 이상 갈 │ │ │ Layered + Spring 그대로 │
        │ 시스템인가?        │ │ │ 가는 게 비용 대비 이득   │
        └────────┬───────────┘ │ └────────────────────────┘
            Yes  │  No         │
                 ▼             ▼
        ┌────────────────┐  ┌──────────────────────────────┐
        │ Hexagonal 적용 │  │ Hexagonal은 과함.            │
        │ 가치 있음      │  │ 도메인 객체만 JPA에서 분리해도 │
        └────────────────┘  │ 충분.                        │
                            └──────────────────────────────┘
```

### SOLID 적용 체크리스트 (역으로)

각 원칙은 "지키자"가 아니라 "**이 고통이 있을 때만 꺼낸다**"로 사용:

| 원칙 | 꺼내는 신호 |
|---|---|
| SRP | 한 클래스를 여러 부서/팀이 동시에 고치려 한다 |
| OCP | 같은 형태의 분기가 *세 번째* 등장했다 |
| LSP | 상속 트리에서 하위 타입이 부모의 약속을 깬다 |
| ISP | 구현체가 메서드 일부에 `UnsupportedOperationException`을 던진다 |
| DIP | 테스트하려는데 구체 클래스가 막고 있다 / 구현이 *실제로* 2개 이상 필요해졌다 |

---

## 4. 한 줄 결론

> **패턴은 도구일 뿐, 신앙이 아니다.**
> SOLID와 헥사고날 둘 다 **실제 고통의 처방전**이지만,
> 그 고통이 없는 코드에 적용하면 **항생제를 평소에 먹는 것**과 같다.
> 거부도 무비판, 도입도 무비판 — 둘 다 같은 종류의 게으름이다.

---

## 5. 더 읽을거리

- Robert C. Martin, *Clean Architecture* (2017) — SOLID + Clean Architecture
- Alistair Cockburn, "Hexagonal Architecture" (2005) — 원 논문
- Tom Hombergs, *Get Your Hands Dirty on Clean Architecture* (2019) — Spring/Java 기준 헥사고날 실전
- Vaughn Vernon, *Implementing Domain-Driven Design* — 헥사고날 + DDD 결합
- Sandi Metz, *Practical Object-Oriented Design* — SOLID를 종교화하지 않는 관점