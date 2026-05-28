package me.victor.spring01.learning.lifecycle

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.BeanNameAware
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component

/**
 * 라이프사이클 훅을 한 번씩 모두 사용해보는 학습용 빈.
 *
 * 기동 시 호출 순서:
 *   1. 생성자
 *   2. BeanNameAware#setBeanName  (그 외 *Aware 인터페이스들도 이 단계)
 *   3. BeanPostProcessor#postProcessBeforeInitialization
 *   4. @PostConstruct                      ← jakarta 표준 (권장)
 *   5. InitializingBean#afterPropertiesSet ← Spring 인터페이스 (강결합)
 *   6. BeanPostProcessor#postProcessAfterInitialization
 *
 * 종료 시 (graceful shutdown 한정 — Ctrl+C, SIGTERM 등):
 *   D1. @PreDestroy
 *   D2. DisposableBean#destroy
 *
 * 실무에서는 보통 @PostConstruct / @PreDestroy 만 쓴다.
 * Spring 인터페이스(Initializing/Disposable Bean)는 클래스를 Spring에 강결합시켜 비권장.
 */
@Component
class LifecycleAwareBean : BeanNameAware, InitializingBean, DisposableBean {

    private val log = LoggerFactory.getLogger(javaClass)
    private var beanName: String = ""

    init {
        log.info("[lifecycle] 1. constructor")
    }

    override fun setBeanName(name: String) {
        beanName = name
        log.info("[lifecycle] 2. BeanNameAware#setBeanName = {}", name)
    }

    @PostConstruct
    fun postConstruct() {
        log.info("[lifecycle] 4. @PostConstruct ({})", beanName)
    }

    override fun afterPropertiesSet() {
        log.info("[lifecycle] 5. InitializingBean#afterPropertiesSet ({})", beanName)
    }

    @PreDestroy
    fun preDestroy() {
        log.info("[lifecycle] D1. @PreDestroy ({})", beanName)
    }

    override fun destroy() {
        log.info("[lifecycle] D2. DisposableBean#destroy ({})", beanName)
    }
}
