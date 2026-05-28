package me.victor.spring01.learning.lifecycle

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LifecycleDemoConfig {

    private val log = LoggerFactory.getLogger("LifecycleDemoRunner")

    @Bean
    fun lifecycleDemoRunner(
        context: ApplicationContext,
        singleton: SingletonCounter,
        issuer: TicketIssuer,
    ): ApplicationRunner = ApplicationRunner {
        // --- singleton: 컨테이너에서 다시 꺼내도 같은 인스턴스 ---
        val again = context.getBean(SingletonCounter::class.java)
        log.info("singleton same instance? {}", singleton === again)
        log.info("singleton counter = {}, {}, {}",
            singleton.increment(), singleton.increment(), singleton.increment())

        // --- prototype: ObjectProvider로 매번 새 인스턴스 ---
        val t1 = issuer.issue()
        val t2 = issuer.issue()
        log.info("prototype ids = {}, {}  (different instance? {})", t1.id, t2.id, t1 !== t2)
    }
}
