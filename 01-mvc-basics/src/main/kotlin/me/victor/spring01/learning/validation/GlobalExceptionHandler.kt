package me.victor.spring01.learning.validation

import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import java.net.URI

/**
 * 전역 예외 처리 — 컨트롤러에서 throw된 예외를 RFC 9457(Problem Details) 형식으로 변환.
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody.
 *   여기서 반환한 ProblemDetail이 자동으로 application/problem+json으로 직렬화된다.
 *
 * 다루는 예외:
 *   - MethodArgumentNotValidException  @Valid @RequestBody 검증 실패
 *   - HandlerMethodValidationException Spring 6.1+ 의 통합 메서드 인자 검증 실패
 *   - ConstraintViolationException     일부 환경에서 path/query 검증 실패 (구 경로)
 *   - UserNotFoundException            도메인 예외 → 404
 *
 * 다루지 않은 예외는 Spring의 기본 핸들러(ResponseEntityExceptionHandler)가
 * spring.mvc.problemdetails.enabled=true 설정 하에서 ProblemDetail로 응답한다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleBodyValidation(ex: MethodArgumentNotValidException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "요청 본문 검증에 실패했습니다.").apply {
            title = "Validation Failed"
            setProperty("errors", ex.bindingResult.fieldErrors.map { fe ->
                mapOf(
                    "field" to fe.field,
                    "message" to fe.defaultMessage,
                    "rejected" to fe.rejectedValue,
                )
            })
        }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleParamValidation(ex: HandlerMethodValidationException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "요청 파라미터 검증에 실패했습니다.").apply {
            title = "Validation Failed"
            setProperty("errors", ex.allValidationResults.flatMap { result ->
                result.resolvableErrors.map { err ->
                    mapOf(
                        "parameter" to (result.methodParameter.parameterName ?: "?"),
                        "message" to err.defaultMessage,
                    )
                }
            })
        }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "요청 파라미터 검증에 실패했습니다.").apply {
            title = "Validation Failed"
            setProperty("errors", ex.constraintViolations.map { cv ->
                mapOf(
                    "path" to cv.propertyPath.toString(),
                    "message" to cv.message,
                    "rejected" to cv.invalidValue,
                )
            })
        }

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(ex: UserNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Not Found").apply {
            title = "User Not Found"
            // type은 에러 카탈로그 URI를 가리키는 게 RFC 9457 권장.
            // 실제 서비스라면 사내 문서의 영구 링크를 박는다.
            type = URI.create("https://example.com/errors/user-not-found")
            setProperty("userId", ex.id)
        }
}
