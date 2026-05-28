package me.victor.spring01.learning.lifecycle

import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

/**
 * 기본 Scope = singleton.
 * 컨테이너당 단 한 개의 인스턴스. 모든 주입 지점이 같은 객체를 본다.
 *
 * 99%의 비즈니스 서비스는 stateless이므로 singleton이 맞다.
 * 상태를 가지는 빈에 무의식적으로 singleton을 쓰면 동시성 버그의 출발점이 된다.
 */
@Component
class SingletonCounter {
    private val count = AtomicLong()
    fun increment(): Long = count.incrementAndGet()
}

/**
 * Prototype Scope.
 * 컨테이너에서 요청할 때마다 새 인스턴스 생성.
 *
 * 주의: prototype 빈을 singleton 빈의 필드로 직접 주입하면, singleton이 만들어질 때
 * "한 번만" prototype 인스턴스가 들어와 박힌다. 그 뒤로는 같은 객체를 보게 된다.
 *
 * 매번 새 인스턴스가 필요하다면 ObjectProvider<T> 패턴을 쓴다 → TicketIssuer.kt 참고.
 */
@Component
@Scope(SCOPE_PROTOTYPE)
class PrototypeTicket {
    val id: Long = SEQUENCE.incrementAndGet()

    companion object {
        private val SEQUENCE = AtomicLong()
    }
}
