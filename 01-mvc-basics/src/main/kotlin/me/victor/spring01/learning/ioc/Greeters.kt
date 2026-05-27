package me.victor.spring01.learning.ioc

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * 같은 타입(Greeter)의 Bean이 여러 개 등록되는 시나리오.
 *
 * - 후보가 여러 개일 때 Spring은 "어느 걸 주입해야 할지" 알 수 없으므로
 *   기본값을 정해주거나(@Primary), 부르는 쪽에서 콕 집어야(@Qualifier) 한다.
 * - 둘 다 없으면 NoUniqueBeanDefinitionException으로 컨테이너 기동 실패.
 */
interface Greeter {
    fun greet(name: String): String
}

@Component
class EnglishGreeter : Greeter {
    override fun greet(name: String) = "Hello, $name!"
}

/**
 * @Primary: 같은 타입 후보가 여러 개일 때, 명시적 선언이 없으면 이게 기본으로 주입된다.
 *   장점: 부르는 쪽 코드가 깔끔.
 *   주의: 남발하면 "어디서 어떤 구현이 오는지" 흐려진다. 정말로 "기본"이라 부를 만한 후보에만 붙일 것.
 */
@Component
@Primary
class KoreanGreeter : Greeter {
    override fun greet(name: String) = "안녕하세요, $name 님!"
}

/**
 * @Qualifier("japanese"): 이 Bean을 "japanese"라는 라벨로 식별 가능하게 한다.
 *   injection point에서 같은 라벨로 콕 집을 수 있게 됨.
 */
@Component
@Qualifier("japanese")
class JapaneseGreeter : Greeter {
    override fun greet(name: String) = "こんにちは、${name} さん!"
}
