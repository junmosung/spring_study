package me.victor.spring01.learning.validation

/**
 * 도메인 예외 — 비즈니스 의미를 담는 명시적 타입.
 *
 * 의도:
 *   - 컨트롤러/서비스 내부에서 throw하면 @RestControllerAdvice의 핸들러가 잡아
 *     적절한 HTTP 상태 + ProblemDetail로 변환한다.
 *   - HTTP 코드를 도메인 코드에 박지 않는다(예: throw 404 ❌). 변환은 web 계층의 책임.
 */
class UserNotFoundException(val id: Long) : RuntimeException("User $id not found")
