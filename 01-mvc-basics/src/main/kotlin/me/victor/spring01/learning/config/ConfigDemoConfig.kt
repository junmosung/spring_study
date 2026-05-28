package me.victor.spring01.learning.config

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Properties 클래스를 컨테이너에 등록하는 방법 — 3가지가 있다:
 *
 *  1. @EnableConfigurationProperties(MyProps::class, ...)  ← 이 파일에서 사용
 *     특정 @Configuration에서 명시적으로 등록. 어떤 properties가 활성되는지 한눈에 보임.
 *
 *  2. @ConfigurationPropertiesScan
 *     메인 클래스(@SpringBootApplication 옆)에 붙이면 base package 하위의
 *     @ConfigurationProperties 클래스를 자동 스캔. 모듈 전반에 properties가 많을 때 편리.
 *
 *  3. @Component를 properties 클래스에 추가
 *     동작은 하지만 시그니처가 섞이고(데이터 vs 컴포넌트) 컨벤션과 어긋남. 비권장.
 *
 * ApplicationRunner를 통해 바인딩 결과를 시작 시 로그로 출력.
 * application.yaml의 app.*, mail.* 섹션이 그대로 토스트링으로 찍히면 성공.
 */
@Configuration
@EnableConfigurationProperties(AppProperties::class, MailProperties::class)
class ConfigDemoConfig {

    private val log = LoggerFactory.getLogger("ConfigDemoRunner")

    @Bean
    fun configDemoRunner(
        app: AppProperties,
        mail: MailProperties,
    ): ApplicationRunner = ApplicationRunner {
        log.info("app  = {}", app)
        log.info("mail = {}", mail)
    }
}
