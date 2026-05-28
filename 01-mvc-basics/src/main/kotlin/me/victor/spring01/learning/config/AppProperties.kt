package me.victor.spring01.learning.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.util.unit.DataSize
import java.time.Duration

/**
 * @ConfigurationProperties — 타입 안전한 설정 바인딩.
 *
 * Kotlin data class + Spring Boot 3.0+ 조합에서는 따로 @ConstructorBinding을 붙일 필요가 없다.
 * (단일 생성자가 있으면 Spring이 알아서 constructor binding을 사용한다.)
 *
 * 등록 방법은 ConfigDemoConfig.kt에서 @EnableConfigurationProperties로 명시.
 * 다른 방법: @ConfigurationPropertiesScan을 메인 클래스에 붙이면 자동 스캔도 가능.
 *
 * 바인딩 규칙 (Relaxed Binding):
 *   application.yaml의 'app.cache-ttl' → cacheTtl (kebab → camel)
 *   환경변수 'APP_CACHE_TTL'           → cacheTtl (SCREAMING_SNAKE → camel)
 *   대소문자·구분자 차이는 모두 같은 키로 본다.
 *
 * 특별 타입:
 *   - java.time.Duration:  "10m", "30s", "PT1H30M" 등 ISO-8601 / 축약형 모두 파싱
 *   - DataSize:            "20MB", "1GB", "512KB" 등
 *   - List, Map, 중첩 클래스 모두 OK
 */
@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val name: String,
    val owner: Owner,
    val features: Features = Features(),
    val tags: List<String> = emptyList(),
    val timeouts: Map<String, Duration> = emptyMap(),
    val cacheTtl: Duration = Duration.ofMinutes(5),
    val maxUploadSize: DataSize = DataSize.ofMegabytes(10),
) {
    data class Owner(
        val name: String,
        val email: String,
    )

    data class Features(
        val signup: Boolean = true,
        val betaUi: Boolean = false,
    )
}
