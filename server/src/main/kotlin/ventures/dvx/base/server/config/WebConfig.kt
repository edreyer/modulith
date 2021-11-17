package ventures.dvx.base.server.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
class WebConfig {

  @Bean
  fun corsWebFilter() : CorsWebFilter {
    val corsConfiguration = CorsConfiguration().apply {
        this.allowedOrigins = listOf("localhost")
      }
    val source: CorsConfigurationSource = UrlBasedCorsConfigurationSource().apply {
      this.registerCorsConfiguration("/**", corsConfiguration)
    }
    return CorsWebFilter(source)
  }

}
