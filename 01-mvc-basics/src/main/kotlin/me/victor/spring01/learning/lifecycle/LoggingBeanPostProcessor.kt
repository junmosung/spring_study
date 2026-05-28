package me.victor.spring01.learning.lifecycle

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.stereotype.Component

/**
 * BeanPostProcessor — 컨테이너에 등록된 "모든" 빈의 초기화 직전·직후에 호출된다.
 *
 * - postProcessBeforeInitialization: @PostConstruct / afterPropertiesSet 이전.
 * - postProcessAfterInitialization:  위 둘이 끝난 직후. 여기서 빈을 다른 객체(예: 프록시)로
 *   바꿔 반환할 수 있다. AOP / @Transactional의 프록시 생성도 BeanPostProcessor를 통해 일어난다.
 *
 * 모든 빈에 대해 호출되기 때문에 너무 시끄럽다. 학습용으로 우리 learning.lifecycle 패키지
 * 빈에 대해서만 로그를 남기도록 필터링한다.
 *
 * 주의: BeanPostProcessor 자신은 다른 일반 빈보다 먼저 만들어진다. 그래서 BPP에 @Autowired로
 * 다른 비즈니스 빈을 받으려 하면, 그 빈은 BPP의 도움 없이 일찍 만들어져 BPP 처리를 못 받는
 * 위치에 놓이게 된다. BPP는 가능한 한 의존성 없는 형태로 두는 게 안전.
 */
@Component
class LoggingBeanPostProcessor : BeanPostProcessor {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any {
        if (isLearningBean(bean)) {
            log.info("[lifecycle] 3. BPP#before  {}", beanName)
        }
        return bean
    }

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (isLearningBean(bean)) {
            log.info("[lifecycle] 6. BPP#after   {}", beanName)
        }
        return bean
    }

    private fun isLearningBean(bean: Any) =
        bean.javaClass.packageName.startsWith("me.victor.spring01.learning.lifecycle")
}
