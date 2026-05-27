package me.victor.spring01.learning.ioc

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

/**
 * @Qualifier로 후보 중 특정 Bean을 콕 집어 주입받는 예.
 *
 * - @Primary가 정한 기본을 우회하고 싶을 때 사용.
 * - 라벨로 매칭. Greeters.kt의 JapaneseGreeter에 @Qualifier("japanese")가 붙어 있어 매칭됨.
 * - 라벨이 없는 경우엔 Bean 이름(기본은 클래스 이름의 lowerCamel)으로 매칭되기도 한다.
 *   예: @Qualifier("englishGreeter") 도 동작.
 */
@Service
class QualifiedGreetingService(
    @Qualifier("japanese") private val greeter: Greeter,
) {
    fun greet(name: String) = greeter.greet(name)
}
