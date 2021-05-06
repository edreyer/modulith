package ventures.dvx.common.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InjectionPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope

/**
 * From: https://medium.com/simars/inject-loggers-in-spring-java-or-kotlin-87162d02d068
 */
@Configuration
class LoggingConfig {

  @Bean
  @Scope("prototype")
  fun logger(injectionPoint: InjectionPoint): Logger {
    return LoggerFactory.getLogger(
      injectionPoint.methodParameter?.containingClass // constructor
        ?: injectionPoint.field?.declaringClass) // or field injection
  }

}
