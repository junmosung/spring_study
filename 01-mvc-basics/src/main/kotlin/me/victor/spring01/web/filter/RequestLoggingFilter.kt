package me.victor.spring01.web.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Servlet Filter — DispatcherServlet 보다 앞단에서 동작.
 *
 * - jakarta.servlet 스펙 위에 살기 때문에 "어떤 Controller가 처리할지"는 모른다.
 *   (HandlerMapping이 아직 안 돌았음)
 * - 따라서 횡단 관심사(공통 로깅, CORS, 압축, 인코딩, 인증 토큰 추출 등)에 적합.
 * - OncePerRequestFilter: forward/include 시 중복 호출 방지를 보장하는 Spring 헬퍼.
 */
class RequestLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val start = System.nanoTime()
        log.info("→ {} {}", request.method, request.requestURI)

        try {
            // 다음 Filter, 최종적으로 DispatcherServlet으로 요청을 넘긴다.
            filterChain.doFilter(request, response)
        } finally {
            // 컨트롤러가 예외를 던지더라도 finally에서 status/소요시간을 남길 수 있다.
            val elapsedMs = (System.nanoTime() - start) / 1_000_000
            log.info("← {} {} status={} elapsed={}ms",
                request.method, request.requestURI, response.status, elapsedMs)
        }
    }
}
