package me.victor.spring01.learning.mvc

import java.time.Instant

/**
 * 요청 / 응답 DTO.
 *
 * Kotlin data class는 Jackson(+ jackson-module-kotlin)이 그대로 직렬화·역직렬화한다.
 * 모듈 의존성에 이미 jackson-module-kotlin이 있어 추가 설정 불필요.
 *
 * 주의:
 *  - 노출 모델(API)과 도메인 엔티티를 같은 클래스로 쓰지 말 것. 변경 결합도가 커진다.
 *  - val로 두면 응답 직렬화 시 setter 없이 getter만으로 처리되어 안전.
 */
data class EchoRequest(
    val message: String,
    val repeat: Int = 1,
)

data class EchoResponse(
    val echo: String,
    val length: Int,
    val receivedAt: Instant = Instant.now(),
)
