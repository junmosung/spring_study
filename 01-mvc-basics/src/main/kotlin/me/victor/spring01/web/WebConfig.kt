package me.victor.spring01.web

import me.victor.spring01.web.filter.RequestLoggingFilter
import me.victor.spring01.web.interceptor.HandlerTimingInterceptor
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {

    /**
     * Filter는 Servlet 컨테이너에 등록한다.
     * FilterRegistrationBean을 쓰면 URL 패턴, 순서(order), 이름을 명시할 수 있다.
     * 그냥 @Component를 붙여도 자동 등록되지만, 순서 제어가 필요하면 이 방식이 명시적이다.
     */
    @Bean
    fun requestLoggingFilter(): FilterRegistrationBean<RequestLoggingFilter> {
        return FilterRegistrationBean(RequestLoggingFilter()).apply {
            addUrlPatterns("/*")
            order = Ordered.HIGHEST_PRECEDENCE // 가장 바깥쪽에서 감싸도록.
        }
    }

    /**
     * Interceptor는 Spring MVC에 등록한다 (DispatcherServlet 내부).
     * addPathPatterns / excludePathPatterns로 핸들러 경로 단위 매칭이 가능하다.
     */
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(HandlerTimingInterceptor())
            .addPathPatterns("/**")
            .excludePathPatterns("/actuator/**") // 헬스체크 등은 제외하는 게 보통.
    }
}
