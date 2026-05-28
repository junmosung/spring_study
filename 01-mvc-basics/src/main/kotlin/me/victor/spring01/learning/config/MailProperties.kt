package me.victor.spring01.learning.config

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * @Validated + jakarta.validation 조합.
 *
 * @ConfigurationProperties 객체는 검증 어노테이션이 붙어 있어도 자동으로 검증되지 않는다.
 * 클래스에 @Validated를 붙여야 기동 시 검증이 실행된다.
 * 검증 실패 시 → BindException으로 컨테이너 기동 거부. "잘못된 설정으로 띄우느니 빨리 죽이자" 정책.
 *
 * build.gradle.kts에 이미 spring-boot-starter-validation 의존성이 있어 추가 설정 불필요.
 *
 * Kotlin data class 멤버에 jakarta 어노테이션을 그냥 붙여도 동작하는 이유:
 *   build.gradle.kts의 freeCompilerArgs에 "-Xannotation-default-target=param-property"가
 *   지정돼 있어, val 파라미터의 어노테이션이 param + property 양쪽에 적용된다.
 *   별도 @field: 접두 없이도 검증이 동작.
 */
@ConfigurationProperties(prefix = "mail")
@Validated
data class MailProperties(
    @NotBlank val host: String,
    @Min(1) @Max(65535) val port: Int,
    @Email val from: String,
)
