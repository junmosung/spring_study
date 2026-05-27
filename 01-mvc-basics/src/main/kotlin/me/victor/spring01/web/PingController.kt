package me.victor.spring01.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Filter / Interceptor 동작을 눈으로 확인하기 위한 최소 컨트롤러.
 *   curl http://localhost:8080/ping
 */
@RestController
class PingController {

    @GetMapping("/ping")
    fun ping(): Map<String, String> = mapOf("message" to "pong")
}
