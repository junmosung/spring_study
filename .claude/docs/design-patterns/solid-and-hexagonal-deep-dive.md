# SOLID와 Hexagonal — 깊이 있는 이해

> 이 문서는 **개념 자체에 대한 심층 정리**입니다.
> 비판적 관점("맹신 경계")은 자매 문서 [`solid-and-hexagonal.md`](./solid-and-hexagonal.md)를 보세요.
> Spring 생태계 패턴 카탈로그는 [`spring-patterns-catalog.md`](./spring-patterns-catalog.md) 참조.

---

## Part 1. SOLID

### 1.1 출처와 정체

- **고안**: Robert C. Martin (a.k.a. Uncle Bob), 2000년 논문 *Design Principles and Design Patterns*에서 5원칙으로 정리.
- **약어 SOLID**: Michael Feathers가 순서를 재배치하며 명명(2004년경).
- **각 원칙은 SOLID 이전부터 따로 존재**한 아이디어를 Uncle Bob이 OOP 맥락에 맞춰 묶은 것:

| 원칙 | 사상적 뿌리 | 출처 |
|---|---|---|
| SRP | 정보 은닉 / 모듈 분해 | David Parnas, 1972, *On the Criteria To Be Used in Decomposing Systems into Modules* |
| OCP | 상속 기반 확장 | Bertrand Meyer, 1988, *Object-Oriented Software Construction* |
| LSP | 행위적 하위 타입 | Barbara Liskov, 1987 OOPSLA keynote, *Data Abstraction and Hierarchy* |
| ISP | Xerox 프린터 사례 | Robert Martin, 1996 |
| DIP | Robert Martin 정립 | Robert Martin, 1996 |

→ SOLID는 **새로운 발명이 아니라 큐레이션**. 그래서 정의가 자료마다 미세하게 다르고, Uncle Bob 본인도 SRP 정의를 여러 번 수정했습니다.

---

### 1.2 각 원칙 — 의도, 메커니즘, 디테일

#### S — Single Responsibility Principle

> **현재 정의(Uncle Bob, 2017)**: "한 모듈은 하나의, 그리고 오직 하나의 *액터*에 대해 책임을 져야 한다."

- **원래 정의**: "변경의 이유가 하나여야 한다" — 이게 너무 모호해서 사람마다 다르게 해석된 게 SRP가 가장 오용되는 원인.
- **"액터"란?**: 모듈에 변경을 요청할 수 있는 사람/그룹. 회계팀, 인사팀, DBA처럼.
- **진짜 의도**:
  - *Shotgun Surgery*(하나 바꾸려면 여러 클래스를 흩어서 고쳐야 함)
  - *Divergent Change*(한 클래스가 여러 이유로 자주 바뀜)
  - 위 두 코드 냄새를 막는 것.
- **응집도(Cohesion)와의 관계**: SRP를 잘 지키면 응집도가 높아지고, 응집도가 너무 낮으면 SRP 위반의 신호.

#### O — Open/Closed Principle

> "소프트웨어 엔티티는 확장에는 열려있고 수정에는 닫혀있어야 한다."

- **Meyer의 원형(1988)**: *상속*으로 확장. 부모 클래스는 변경 안 하고 자식이 확장.
- **Polymorphic OCP(Uncle Bob)**: *추상화*로 확장. 인터페이스에 의존, 새 기능은 새 구현체 추가.
- **메커니즘**:
  - Strategy 패턴: 알고리즘을 갈아끼움
  - Template Method: 골격은 닫혀있고 hook만 열어둠
  - Plugin / Visitor / Observer 등도 OCP의 발현
- **현실적 한계**: 미래의 확장 축을 *정확히* 예측할 수 있어야 OCP가 가치 있음. 잘못 예측한 OCP는 영원한 부담.

#### L — Liskov Substitution Principle

> "S가 T의 하위 타입이라면, T의 객체를 S의 객체로 치환해도 프로그램의 정합성은 유지되어야 한다."

- **Liskov-Wing 형식(1994)** — 가장 엄격한 정의:
  - **사전조건**(precondition): 하위 타입이 *더 강화하면 안 됨* (인자 제약을 추가하면 깨짐)
  - **사후조건**(postcondition): 하위 타입이 *더 약화하면 안 됨* (보장하던 결과를 빼면 깨짐)
  - **불변식**(invariant): 부모의 불변식을 유지
  - **history rule**: 부모가 허용하지 않는 상태 변화를 자식이 도입하면 안 됨
- **고전 예시**: 정사각형은 사각형의 하위 타입이 되어선 안 된다. 부모는 width/height 독립 변경을 약속하지만 정사각형은 그 약속을 깬다.
- **실무 신호**: `instanceof` 분기로 자식 타입을 분류하는 코드가 보이면 LSP가 깨졌다는 강한 신호.

#### I — Interface Segregation Principle

> "클라이언트는 자신이 사용하지 않는 메서드에 의존하도록 강요받지 않아야 한다."

- **Xerox 사례**: Uncle Bob이 컨설팅한 Xerox 프린터 소프트웨어. 한 거대 `Job` 클래스가 인쇄/스테이플/팩스 등 모든 능력을 가지고 있었고, 인쇄만 쓰는 클라이언트도 모든 변경의 영향을 받음. 능력별 인터페이스로 쪼개서 해결.
- **본질**: *Fat Interface* 회피. 인터페이스를 **클라이언트 관점**으로 설계.
- **DIP와의 관계**: DIP가 "구체가 아닌 추상에 의존"이라면, ISP는 "**올바른 모양의 추상**에 의존" — 두 원칙은 짝.

#### D — Dependency Inversion Principle

> "상위 모듈은 하위 모듈에 의존하지 않는다. 둘 다 추상에 의존해야 한다. 추상은 세부사항에 의존하지 않는다. 세부사항이 추상에 의존해야 한다."

- **"역전(Inversion)"의 뜻**: 전통적 호출 흐름에서는 *상위가 하위를 직접 import*. DIP를 적용하면 *추상의 소유권*이 상위(정책)로 옮겨가서, 하위(세부)가 그 추상을 구현하는 방향으로 의존이 뒤집힘.

```
[전통적]                     [DIP 적용]

상위 정책                     상위 정책 ── 정의 ──▶ 추상(Interface)
   │                                                  ▲
   ▼                                                  │
하위 세부                     하위 세부 ── 구현 ──────┘
```

- **DI(Dependency Injection)와의 혼동**: DIP는 *원칙*, DI는 *DIP를 구현하는 기법*. Spring의 `@Autowired`는 DI 도구일 뿐, DIP를 강제하지는 않음.
- **IoC(Inversion of Control)와의 혼동**: IoC는 더 넓은 개념(제어 흐름의 역전). DI는 IoC를 객체 생성에 한정해 적용한 것.

---

### 1.3 SOLID 사이의 긴장

원칙들이 서로 충돌할 수 있다는 점이 자주 간과됩니다:

- **SRP ↔ ISP**: SRP를 극단으로 밀면 클래스가 잘게 쪼개지고, ISP에 부합하지만 *navigability*가 떨어짐.
- **OCP ↔ YAGNI**: OCP를 미리 적용하려면 미래를 예측해야 하는데, YAGNI는 "예측하지 말라"고 함.
- **DIP ↔ Simplicity**: 구현이 1개뿐인데 DIP를 적용하면 indirection만 늘어남.

→ 결론: SOLID는 **체크리스트가 아니라 어휘**. 코드 냄새를 진단할 때 쓰는 용어로 활용하는 것이 본래 의도.

---

## Part 2. Hexagonal Architecture

### 2.1 출처와 동기

- **고안**: Alistair Cockburn, 2005년 *Hexagonal architecture* 글로 정식 발표 (작업 시작은 1990년대 후반).
- **공식 명칭**: *Ports and Adapters Architecture* — "Hexagonal"은 단지 그림에서 6각형으로 그리면 sides가 늘어나도 표현하기 쉬워서 붙은 이름이지, 6이라는 숫자 자체에는 의미가 없음.
- **해결하려던 고통**:
  1. 비즈니스 로직이 UI 코드 안에 박혀서 테스트 불가능
  2. 같은 비즈니스 로직을 GUI/CLI/배치/테스트에서 *동등하게* 호출할 수 없음
  3. DB나 외부 API가 바뀌면 도메인까지 흔들림
- **Cockburn의 핵심 통찰**: "**비대칭이 문제다**" — 전통적으로는 UI(위)와 DB(아래)를 다르게 취급했지만, 도메인 입장에서 둘은 **똑같이 외부**다. 좌/우 또는 안/밖으로 보면 대칭으로 다룰 수 있다.

---

### 2.2 핵심 개념

#### Port (포트)

도메인이 외부와 소통하기 위해 정의하는 **인터페이스**. 두 종류:

| 종류 | 다른 이름 | 의미 | 예시 |
|---|---|---|---|
| **Driving Port** | Primary, Inbound, Use-case | 외부가 도메인을 *호출하기 위해* 도메인이 노출하는 입구 | `PlaceOrderUseCase` |
| **Driven Port** | Secondary, Outbound | 도메인이 외부에게 *요구하는* 능력 | `OrderRepository`, `PaymentGateway`, `NotificationSender` |

#### Adapter (어댑터)

포트를 실제 기술로 연결하는 **구현체**. 역시 두 종류:

| 종류 | 의미 | 예시 |
|---|---|---|
| **Driving Adapter** | 외부 트리거(HTTP/CLI/메시지)를 받아 Driving Port를 호출 | REST 컨트롤러, Kafka 컨슈머, 스케줄러 |
| **Driven Adapter** | Driven Port의 구현. 도메인의 요구를 실제 기술로 수행 | JPA Repository 구현체, REST 클라이언트, SMTP 전송기 |

#### Dependency Rule

```
       Adapter ──의존──▶ Port ──정의──▶ Domain
                              ▲
                              │
                       (Port는 Domain이 소유)
```

- 화살표 방향: 안쪽으로만 흐름.
- 도메인은 어떤 어댑터도 모름. 심지어 *언어 차원에서* import 0건이어야 함.
- 이게 깨지면 Hexagonal이 아님 (폴더만 헥사고날인 경우의 진단법).

---

### 2.3 친척 아키텍처와의 관계

세 아키텍처는 본질적으로 같은 사상의 다른 표현입니다:

| 아키텍처 | 고안자/연도 | 강조점 |
|---|---|---|
| **Hexagonal / Ports & Adapters** | Cockburn, 2005 | *대칭성* — 모든 외부는 동등하게 어댑터로 다룸 |
| **Onion Architecture** | Jeffrey Palermo, 2008 | *동심원* — 안쪽이 더 안정적, 바깥일수록 휘발성 |
| **Clean Architecture** | Robert C. Martin, 2012 | *Entities/Use Cases/Adapters/Frameworks*로 4겹 분리, **Dependency Rule** 명문화 |

세 아키텍처를 거칠게 비교하면:

```
Hexagonal:    Domain ◀── Port ── Adapter ── World
              (좌우 / 안밖 대칭)

Onion:        Core ── Domain Services ── Application ── Infrastructure
              (동심원, 안쪽이 핵)

Clean:        Entities ── Use Cases ── Interface Adapters ── Frameworks/Drivers
              (4겹, 화살표는 안쪽으로만)
```

→ 실무에서는 거의 같은 폴더 구조로 귀결됨. 어휘만 다름.

---

### 2.4 자주 묻는 디테일

#### Q1. 도메인 모델은 어떤 모양이어야 하나?

- **POJO(Plain Old Java/Kotlin Object)**여야 함. Spring/JPA 어노테이션 0건.
- 비즈니스 규칙은 도메인 객체의 메서드로 들어가야 함(=*Rich Domain Model*).
- 흔한 실수: 도메인 객체가 `@Entity`이고 메서드는 하나도 없음. 이건 *Anemic Domain Model* — 헥사고날의 가치 80%가 사라짐.

#### Q2. DTO/Domain/Entity는 어떻게 분리하나?

```
HTTP Request (Adapter)
       │ JSON → DTO
       ▼
   PlaceOrderCommand (Application)
       │ DTO → Domain
       ▼
   Order (Domain)
       │ Domain → JPA Entity
       ▼
   OrderJpaEntity (Adapter)
       │ ORM
       ▼
   DB
```

- 보일러플레이트가 늘지만, 각 경계에서 *변환 자유도*를 얻음.
- Kotlin이면 `data class` + 확장함수로 변환 비용을 크게 줄일 수 있음.

#### Q3. 트랜잭션 경계는 어디에?

- **Application Service(Driving Port 구현)**에 두는 게 정석.
- 도메인 자체는 트랜잭션을 모름.
- Spring `@Transactional`은 application service에만.

#### Q4. CQRS와의 조합?

- Hexagonal은 Command/Query 어느 쪽이든 적용 가능.
- 보통 Command 쪽이 도메인 모델 가치가 크고, Query 쪽은 단순 read model로 우회(헥사고날 우회). 이게 *Read/Write 분리*의 자연스러운 동기.

---

## 더 읽을거리

- Robert C. Martin, *Clean Architecture* (2017)
- Robert C. Martin, *Agile Software Development: Principles, Patterns, and Practices* (2002) — SOLID 원전
- Alistair Cockburn, "Hexagonal Architecture" (2005) — 원 논문
- Tom Hombergs, *Get Your Hands Dirty on Clean Architecture* (2019) — Spring/Java 기준 헥사고날 실전
- Vaughn Vernon, *Implementing Domain-Driven Design* (2013) — 헥사고날 + DDD 결합
- Barbara Liskov, Jeannette Wing, *A Behavioral Notion of Subtyping* (1994) — LSP 원전