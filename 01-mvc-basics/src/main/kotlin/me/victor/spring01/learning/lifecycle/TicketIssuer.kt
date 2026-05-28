package me.victor.spring01.learning.lifecycle

import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service

/**
 * Prototype 빈을 singleton 서비스에서 "매번 새로" 받고 싶을 때의 정석.
 *
 * - PrototypeTicket을 그냥 필드로 주입하면 TicketIssuer가 생성될 때 단 한 번만 들어와 박힌다.
 * - ObjectProvider<T>를 주입받으면 getObject() 호출 시점마다 컨테이너에 요청해서 새 인스턴스를 받는다.
 *
 * 대안:
 *  - jakarta.inject.Provider<T> (CDI 표준, 동일하게 동작)
 *  - ApplicationContext.getBean(...) 직접 호출 (테스트 어렵고 의존성이 흐려져 비권장)
 *  - @Lookup 메서드 (CGLIB 기반, 좀 더 magical)
 */
@Service
class TicketIssuer(
    private val tickets: ObjectProvider<PrototypeTicket>,
) {
    fun issue(): PrototypeTicket = tickets.getObject()
}
