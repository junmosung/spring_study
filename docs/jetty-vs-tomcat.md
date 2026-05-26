# Embedded Servlet Container — Tomcat → Jetty 전환 메모

학습 목적으로 기본 임베디드 컨테이너를 Tomcat에서 Jetty로 교체했다. 이 문서는 (1) 무엇을 어떻게 바꿨는지, (2) Jetty를 택했을 때의 이점, (3) Tomcat과 Jetty의 구조적 차이를 정리한다.

## 1. 변경 내역

`build.gradle.kts`에서 `spring-boot-starter-web`의 기본 Tomcat 모듈을 제외하고 `spring-boot-starter-jetty`를 추가했다.

```kotlin
implementation("org.springframework.boot:spring-boot-starter-web") {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
}
implementation("org.springframework.boot:spring-boot-starter-jetty")
```

런타임 클래스패스 확인 (`./gradlew dependencies --configuration runtimeClasspath`):

```
spring-boot-starter-jetty -> 4.0.6
  └─ jetty-server / jetty-http / jetty-io / jetty-util  : 12.1.8
  └─ jetty-ee11-webapp / jetty-ee11-servlet            : 12.1.8 (Jakarta EE 11)
  └─ jetty-ee11-websocket-jakarta-server               : 12.1.8
tomcat-embed-el : 11.0.21  ← EL(Expression Language)만 남음. 서블릿 컨테이너 아님
```

`tomcat-embed-core`, `tomcat-embed-websocket` 등 **서블릿 컨테이너로서의 Tomcat은 사라졌고**, EL 구현체만 Jetty가 끌어와 함께 사용한다(Jakarta EL 표준 구현이 Tomcat 산하라서 그렇다).

## 2. 부팅 확인

```
o.s.b.j.s.JettyServletWebServerFactory  : Server initialized with port: 8080
org.eclipse.jetty.server.Server         : jetty-12.1.8; built: 2026-04-01T01:09:18.991Z; jvm 24.0.1
o.e.jetty.server.AbstractConnector      : Started oejs.ServerConnector@...{HTTP/1.1, (http/1.1)}{0.0.0.0:8080}
o.s.boot.jetty.JettyWebServer           : Jetty started on port 8080 (http/1.1) with context path '/'
Started Spring01ApplicationKt in 1.877 seconds
```

HTTP 응답 헤더 (컨트롤러 없이 / 호출):
```
HTTP/1.1 404 Not Found
Cache-Control: must-revalidate,no-cache,no-store
Content-Type: application/json
```

`Server: Tomcat`이 사라지고 Jetty 12 응답 포맷으로 바뀐 것을 확인.

## 3. Jetty로 갈 때의 이점 (이 프로젝트 맥락)

- **학습 가치** — Spring Boot의 임베디드 컨테이너 추상화(`ServletWebServerFactory`)가 어떻게 구현체 교체를 허용하는지를 직접 체험할 수 있다. starter 한 줄 교체로 끝난다는 사실 자체가 핵심 학습 포인트.
- **Eclipse 거버넌스 / 작은 코어** — Jetty는 Eclipse Foundation에서 관리하며 Tomcat 대비 핵심 코드가 작다. 의존성 트리도 모듈로 잘게 쪼개져 있어(`jetty-server`, `jetty-http`, `jetty-io`, `jetty-util` …) 필요한 것만 골라쓰기 좋다.
- **임베디드 워크로드에 일찍부터 최적화된 설계** — Jetty는 초기부터 라이브러리로 임베디드되는 형태를 1차 사용 사례로 잡고 발전했다(예: 구글 인프라, Hadoop, Spark UI). 그래서 부팅이 가볍고 메모리 풋프린트가 작은 편.
- **표준화된 비동기 I/O 모델** — Jetty는 자체 EatWhatYouKill 스레드 모델로 NIO 이벤트 루프 위에서 짧은 요청을 직접 처리, 긴 요청만 워커로 보낸다. 짧은 요청 위주의 API 서버에서 컨텍스트 스위치를 줄이는 데 유리.
- **HTTP/3(QUIC) 정식 지원이 빠른 편** — Tomcat은 Jetty 대비 QUIC 지원이 늦었다. 향후 HTTP/3 실험을 한다면 Jetty 쪽이 진입장벽이 낮다.
- **Reactive 전환 시에도 동일 벤더 유지 가능** — Spring WebFlux로 옮길 때 `spring-boot-starter-reactor-netty` 외에 Jetty Reactive HttpClient/Server도 옵션. Tomcat은 WebFlux 영역에서 Jetty/Netty/Undertow보다 존재감이 약하다.

> 단점도 분명히 있다: 운영 현장에서 Tomcat이 압도적으로 많이 쓰이므로 트러블슈팅 자료/사례가 더 풍부하고, Tomcat의 APR 커넥터(네이티브) 같은 옵션이 없다. **운영 배포가 목표라면 Tomcat이 무난한 기본값이고, Jetty는 의식적인 선택이다.**

## 4. Tomcat vs Jetty 핵심 차이

| 항목 | Tomcat (`spring-boot-starter-tomcat`) | Jetty (`spring-boot-starter-jetty`) |
| --- | --- | --- |
| 거버넌스 | Apache Software Foundation | Eclipse Foundation |
| 현재 버전 (Spring Boot 4.0.6) | Tomcat 11.0.x | Jetty 12.1.x |
| Jakarta EE 레벨 | EE 11 | EE 11 (`jetty-ee11-*` 모듈) |
| 주된 출발점 | 표준 서블릿 컨테이너 (스탠드얼론 톰캣) | 임베디드 HTTP 서버 / 라이브러리 |
| 스레드 모델 | NIO + 워커 풀, 기본 BIO 호환 | NIO + EatWhatYouKill (선택적 가상스레드) |
| 네이티브 커넥터 | APR/OpenSSL 커넥터 옵션 | 없음 (JSSE/Conscrypt) |
| HTTP/2 | 지원 | 지원 |
| HTTP/3 (QUIC) | 12 부분 지원, 성숙도는 Jetty가 앞섬 | 정식 지원 (실험에서 표준화로 빠르게 이동) |
| WebSocket | `tomcat-embed-websocket` | `jetty-ee11-websocket-jakarta-server` |
| 설정 진입점 | `server.tomcat.*` | `server.jetty.*` |
| 자체 모니터링 | JMX MBeans (Tomcat 전용) | JMX MBeans (Jetty 전용) — 노출 빈자리는 서로 다름 |
| 운영 사례 / 자료량 | 매우 많음 | 적은 편 (단, 임베디드 사용처는 많음) |
| 빌드 산출물 크기 | 비슷 (실제 차이는 미미) | 비슷 |
| 시작 시간 | 거의 동일 (소수 백 ms 차이, 환경 영향이 더 큼) | 거의 동일 |

> 흔히 "Jetty가 더 빠르다 / Tomcat이 더 빠르다" 식의 절대 비교가 돌아다니지만, 실제로는 워크로드/설정/JVM 버전에 따라 결과가 뒤집힌다. 표를 만들기 위한 항목으로 시작 시간을 넣었지만 **이 프로젝트의 1.5~2초대 부팅 시간 차이는 컨테이너 선택보다 JVM 워밍업과 클래스 로딩 비용이 더 지배적이다.**

### Spring Boot가 가리고 있는 부분

Spring Boot의 자동구성(`spring-boot-starter-web` → 기본 Tomcat, 또는 Jetty/Undertow로 교체)이 다음을 자동으로 해준다.

- 임베디드 컨테이너 빈 등록 (`ServletWebServerFactory`)
- `DispatcherServlet` 매핑 (`/`)
- 기본 에러 핸들러, 정적 리소스 매핑
- `server.port` 등의 공통 설정 → 컨테이너별 어댑터 호출

덕분에 `Tomcat → Jetty` 전환에 코드 수정이 전혀 필요 없었다. 컨테이너 종속 설정(스레드풀 튜닝 등)을 쓰지 않는 한 동일하다.

## 5. 후속으로 알아두면 좋은 설정

이번 프로젝트에는 아직 적용하지 않았지만, 운영 단계로 갈 때 자주 만지는 키들.

```yaml
server:
  jetty:
    threads:
      max: 200            # 워커 스레드 상한
      min: 8              # 워커 최소
      idle-timeout: 60s   # 유휴 스레드 종료 시간
      max-queue-capacity: -1   # -1 이면 무제한 (조심)
    accesslog:
      enabled: false
    connection-idle-timeout: 30s
```

(Tomcat 시절엔 동일한 키가 `server.tomcat.threads.*`, `server.tomcat.accesslog.*` 였다.)

가상 스레드(JDK 21+):
```yaml
spring:
  threads:
    virtual:
      enabled: true
```
Spring Boot 4.0은 가상 스레드를 Jetty/Tomcat 양쪽에 동일 옵션으로 노출한다. Jetty 12는 내부 `QueuedThreadPool`을 가상 스레드 기반으로 동작시키도록 자연스럽게 받쳐준다.

## 6. 되돌리고 싶을 때

`build.gradle.kts`에서 두 줄만 되돌리면 된다.

```kotlin
implementation("org.springframework.boot:spring-boot-starter-web")
// (spring-boot-starter-jetty 및 exclude 블록 제거)
```

Spring Boot가 다시 Tomcat 자동구성을 적용한다.

## 정리

- 임베디드 컨테이너 교체는 starter 한 줄 단위의 결정이고, Spring Boot 추상화 덕분에 코드 변경이 필요 없다.
- 이 프로젝트는 학습/실험 목적이므로 운영 안정성보다 구조 이해와 호기심에 가중치를 두고 Jetty로 갔다.
- 운영을 목표로 한 프로덕션 서비스라면 Tomcat이 여전히 가장 안전한 기본값이며, Jetty는 임베디드/라이브러리적 사용·HTTP/3·작은 코어 거버넌스 같은 명확한 이유가 있을 때 선택한다.
