package me.victor.spring01.learning.validation

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.atomic.AtomicLong

/**
 * 검증이 트리거되는 위치 4가지를 모두 노출.
 *
 *   @Valid @RequestBody              → MethodArgumentNotValidException
 *   클래스 @Validated + @PathVariable  → HandlerMethodValidationException (Spring 6.1+) /
 *   클래스 @Validated + @RequestParam     ConstraintViolationException (옛 경로, 일부 환경)
 *   throw 도메인 예외                  → @ExceptionHandler에서 변환
 *
 * @Valid (jakarta) vs @Validated (Spring):
 *   - @Valid: jakarta 표준. @RequestBody·필드 cascading에 사용.
 *   - @Validated: Spring 확장. 클래스 레벨에서 path/query 메서드 인자 검증을 활성화,
 *     그리고 validation group 지정이 가능.
 */
@RestController
@RequestMapping("/api/users")
@Validated // ← 이게 없으면 path/query에 붙은 jakarta 어노테이션이 동작하지 않는다
class UserController {

    private val seq = AtomicLong()

    /** @Valid → body 전체 검증. 실패 시 MethodArgumentNotValidException. */
    @PostMapping
    fun create(@Valid @RequestBody req: CreateUserRequest): UserResponse =
        UserResponse(seq.incrementAndGet(), req.username, req.email, req.age)

    /**
     * @PathVariable에 @Min 적용.
     * 클래스에 @Validated가 있어야 동작한다 (없으면 검증이 그냥 무시됨).
     *
     * id == 404 인 경우 도메인 예외를 던져 ExceptionHandler 경로도 같이 확인.
     */
    @GetMapping("/{id}")
    fun get(@PathVariable @Min(value = 1, message = "id는 1 이상이어야 합니다.") id: Long): UserResponse {
        if (id == 404L) throw UserNotFoundException(id)
        return UserResponse(id, "user$id", "user$id@example.com", 25)
    }

    /** @RequestParam에 @Email 적용. 클래스 @Validated 필요. */
    @GetMapping("/search")
    fun search(@RequestParam @Email(message = "검색 email 형식이 올바르지 않습니다.") email: String): UserResponse =
        UserResponse(0, "search-result", email, 25)
}
