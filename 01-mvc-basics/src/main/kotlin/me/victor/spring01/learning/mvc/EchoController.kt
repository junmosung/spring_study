package me.victor.spring01.learning.mvc

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Spring MVC 요청 흐름을 다양한 입출력 형태로 보여주는 컨트롤러.
 *
 * - @RestController = @Controller + @ResponseBody — 모든 메서드의 반환값이
 *   View가 아닌 HTTP body로 직접 직렬화된다 (HttpMessageConverter).
 * - @RequestMapping을 클래스에 붙이면 모든 메서드 경로에 prefix가 붙는다.
 *
 * 매핑된 엔드포인트:
 *   GET    /api/echo                       — @RequestParam (쿼리스트링)
 *   GET    /api/echo/{id}                  — @PathVariable + @RequestHeader
 *   GET    /api/echo/{id}/preview          — Content negotiation (Accept 헤더로 분기)
 *   POST   /api/echo                       — @RequestBody (JSON → 객체)
 *   DELETE /api/echo/{id}                  — ResponseEntity로 상태/헤더 명시
 */
@RestController
@RequestMapping("/api/echo")
class EchoController {

    /** 쿼리스트링 바인딩. defaultValue로 누락 시 기본값 제공. */
    @GetMapping
    fun echo(
        @RequestParam(defaultValue = "hello") msg: String,
        @RequestParam(defaultValue = "1") times: Int,
    ): EchoResponse {
        val echoed = msg.repeat(times)
        return EchoResponse(echo = echoed, length = echoed.length)
    }

    /**
     * 경로변수 + 헤더 바인딩.
     * required=false + nullable 타입이면 헤더 없을 때 null이 들어온다.
     */
    @GetMapping("/{id}")
    fun echoPath(
        @PathVariable id: Long,
        @RequestHeader(name = "X-User-Agent-Note", required = false) note: String?,
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "note" to note,
        "echo" to "echoing #$id",
    )

    /**
     * Content Negotiation 데모 — 같은 경로에 produces만 다른 두 메서드.
     * Spring이 요청의 Accept 헤더를 보고 매칭되는 쪽을 선택한다.
     *
     *   curl -H 'Accept: application/json' .../preview → JSON
     *   curl -H 'Accept: text/plain'       .../preview → plain text
     *
     * 매칭되는 produces가 없으면 406 Not Acceptable.
     */
    @GetMapping("/{id}/preview", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun previewJson(@PathVariable id: Long): EchoResponse =
        EchoResponse(echo = "preview json for #$id", length = 0)

    @GetMapping("/{id}/preview", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun previewText(@PathVariable id: Long): String =
        "preview text for #$id"

    /**
     * @RequestBody — HTTP body를 Jackson이 EchoRequest 데이터 클래스로 역직렬화.
     * Content-Type이 application/json이어야 매칭 (기본 컨버터 기준).
     */
    @PostMapping
    fun echoBody(@RequestBody req: EchoRequest): EchoResponse {
        val echoed = req.message.repeat(req.repeat)
        return EchoResponse(echo = echoed, length = echoed.length)
    }

    /**
     * ResponseEntity로 상태 코드 + 헤더를 명시.
     * 본문이 없으면 Void / Unit 반환 + .build().
     *
     * 직접 반환(EchoResponse)과의 차이: 상태/헤더 커스터마이즈가 필요한 경우에만 ResponseEntity 사용.
     * 항상 200 OK + body만 보내는 흔한 케이스는 ResponseEntity로 감쌀 필요 없음.
     */
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> =
        ResponseEntity
            .noContent() // 204
            .header("X-Deleted-Id", id.toString())
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .build()
}
