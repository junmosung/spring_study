package me.victor.spring01.web.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView

/**
 * Spring MVC HandlerInterceptor — DispatcherServlet "안"에서 동작.
 *
 * Filter와의 결정적 차이: HandlerMapping이 끝난 뒤이기 때문에
 * "어떤 컨트롤러 메서드가 호출될지"(HandlerMethod)를 알 수 있다.
 * 따라서 핸들러 단위 로직(권한 어노테이션 검사, 핸들러별 metric, ModelAndView 가공)에 적합.
 *
 * 훅 3개:
 *   - preHandle:        컨트롤러 호출 직전. false 반환 시 체인 중단.
 *   - postHandle:       컨트롤러 정상 반환 후, View 렌더링 직전.
 *                       예외가 났으면 호출되지 않음.
 *   - afterCompletion:  View 렌더링까지 끝난 뒤(또는 예외 발생 시)에도 호출. 정리 작업용.
 */
class HandlerTimingInterceptor : HandlerInterceptor {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        // 정적 리소스 핸들러는 HandlerMethod가 아닐 수 있으므로 타입 체크.
        if (handler is HandlerMethod) {
            log.debug("preHandle: {}#{}",
                handler.beanType.simpleName, handler.method.name)
        }
        // 핸들러 단위 소요 시간 측정을 위해 시작 시각을 request scope에 저장.
        request.setAttribute(START_TIME, System.nanoTime())
        return true // false면 컨트롤러가 호출되지 않는다.
    }

    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?,
    ) {
        // 여기서 modelAndView를 수정해 공통 모델 속성을 추가할 수도 있다.
        // 예외가 발생한 경우엔 호출되지 않으므로 정리 로직에는 부적합.
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        val start = request.getAttribute(START_TIME) as? Long ?: return
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        if (handler is HandlerMethod) {
            log.info("handler {}#{} elapsed={}ms ex={}",
                handler.beanType.simpleName, handler.method.name, elapsedMs, ex?.javaClass?.simpleName)
        }
    }

    companion object {
        private const val START_TIME = "HandlerTimingInterceptor.start"
    }
}
