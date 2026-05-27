package me.victor.spring01.learning.ioc

import org.springframework.stereotype.Service
import java.time.Clock
import java.time.format.DateTimeFormatter

/**
 * Constructor Injection — Spring이 권장하는 방식.
 *
 * 이점:
 *  - val 불변. 객체가 항상 완전한 상태로 존재 (반쪽짜리 객체 없음).
 *  - 의존성이 생성자 시그니처에 노출 → "이 클래스가 뭘 필요로 하는지" 한눈에 보임.
 *  - 테스트 시 mock을 그냥 생성자로 넘기면 됨 (reflection 불필요).
 *  - 순환 의존성이 컴파일/기동 시점에 강제로 드러난다 (field/setter 주입에선 우회됨).
 *  - Spring 4.3+ 부터 단일 생성자라면 @Autowired 생략 가능.
 *
 * Greeter의 후보는 EnglishGreeter / KoreanGreeter / JapaneseGreeter 세 개.
 * KoreanGreeter에 @Primary가 붙어 있어 별다른 표시 없이는 그것이 주입된다.
 *
 * Clock은 ClockConfig의 @Bean 메서드가 만든 인스턴스가 주입된다.
 */
@Service
class GreetingService(
    private val greeter: Greeter,
    private val clock: Clock,
) {
    fun greetNow(name: String): String {
        val time = DateTimeFormatter.ISO_LOCAL_TIME.format(clock.instant().atZone(clock.zone))
        return "[$time] ${greeter.greet(name)}"
    }
}
