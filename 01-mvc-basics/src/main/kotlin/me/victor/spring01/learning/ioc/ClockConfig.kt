package me.victor.spring01.learning.ioc

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/**
 * @Bean 스타일 등록.
 *
 * 언제 @Component 대신 @Bean을 쓰나?
 *  - 우리가 직접 작성하지 않은 클래스(예: java.time.Clock, ObjectMapper, DataSource)
 *  - 인스턴스 만드는 데 조건 분기·계산이 필요한 경우 ("dev면 A, prod면 B")
 *  - 같은 타입의 Bean을 여러 변형으로 등록하고 싶을 때 (메서드 여러 개)
 *
 * 메서드 이름이 곧 Bean 이름이 된다. 여기선 "systemClock".
 */
@Configuration
class ClockConfig {

    @Bean
    fun systemClock(): Clock = Clock.systemDefaultZone()
}
