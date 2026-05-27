package me.victor.spring01.learning.ioc

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 애플리케이션 시작 시 IoC 데모 결과를 로그로 출력한다.
 *
 * ApplicationRunner는 컨텍스트 refresh가 끝나고 "Started ..." 로그 직전에 호출된다.
 * 따라서 bootRun 로그에서 "Started Spring01Application" 위쪽에 다음 두 줄이 보이면
 * 의존성 주입이 실제로 잘 일어났다는 뜻:
 *
 *   IocDemoRunner : primary greeting   = [HH:mm:ss] 안녕하세요, victor 님!
 *   IocDemoRunner : qualified greeting = こんにちは、victor さん!
 */
@Configuration
class IocDemoConfig {

    private val log = LoggerFactory.getLogger("IocDemoRunner")

    @Bean
    fun iocDemoRunner(
        primary: GreetingService,
        qualified: QualifiedGreetingService,
    ): ApplicationRunner = ApplicationRunner {
        log.info("primary greeting   = {}", primary.greetNow("victor"))
        log.info("qualified greeting = {}", qualified.greet("victor"))
    }
}
