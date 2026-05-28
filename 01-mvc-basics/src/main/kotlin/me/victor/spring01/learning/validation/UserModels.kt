package me.victor.spring01.learning.validation

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 요청 DTO와 검증 어노테이션.
 *
 * Kotlin data class에서 어노테이션 타깃:
 *   build.gradle.kts의 freeCompilerArgs에 "-Xannotation-default-target=param-property"가 있어
 *   val 파라미터에 어노테이션을 그냥 붙여도 property(getter)에도 적용된다.
 *   → 별도 @field: / @get: 접두 없이 jakarta validation이 동작.
 *
 * 중첩 검증:
 *   address 같은 중첩 객체도 같이 검증하고 싶다면 그 필드에 @Valid를 한 번 더 붙여야 한다.
 */
data class CreateUserRequest(
    @NotBlank(message = "username은 필수입니다.")
    @Size(min = 3, max = 20, message = "username은 3~20자여야 합니다.")
    val username: String,

    @NotBlank
    @Email(message = "올바른 이메일 형식이어야 합니다.")
    val email: String,

    @Min(value = 14, message = "14세 이상이어야 합니다.")
    val age: Int,

    @Valid  // ← 중첩 객체 검증을 cascading 하려면 필수
    val address: AddressDto? = null,
)

data class AddressDto(
    @NotBlank(message = "city는 필수입니다.")
    val city: String,

    @NotBlank(message = "street는 필수입니다.")
    val street: String,
)

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val age: Int,
)
