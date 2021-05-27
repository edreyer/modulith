package ventures.dvx.config

import org.axonframework.commandhandling.CommandBus
import org.axonframework.common.caching.Cache
import org.axonframework.common.caching.WeakReferenceCache
import org.axonframework.config.EventProcessingConfigurer
import org.axonframework.eventhandling.EventBus
import org.axonframework.messaging.Message
import org.axonframework.messaging.interceptors.LoggingInterceptor
import org.axonframework.queryhandling.QueryBus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile


@Configuration
class AxonConfig {
  @Bean
  fun loggingInterceptor(): LoggingInterceptor<Message<*>> {
    return LoggingInterceptor()
  }

  @Autowired
  fun configureLoggingInterceptorFor(
    commandBus: CommandBus,
    loggingInterceptor: LoggingInterceptor<Message<*>>
  ) {
    commandBus.registerDispatchInterceptor(loggingInterceptor)
    commandBus.registerHandlerInterceptor(loggingInterceptor)
  }

  @Autowired
  fun configureLoggingInterceptorFor(eventBus: EventBus, loggingInterceptor: LoggingInterceptor<Message<*>>) {
    eventBus.registerDispatchInterceptor(loggingInterceptor)
  }

  @Autowired
  fun configureLoggingInterceptorFor(
    eventProcessingConfigurer: EventProcessingConfigurer,
    loggingInterceptor: LoggingInterceptor<Message<*>>
  ) {
    eventProcessingConfigurer.registerDefaultHandlerInterceptor { config: org.axonframework.config.Configuration, processorName: String -> loggingInterceptor }
  }

  @Autowired
  fun configureLoggingInterceptorFor(queryBus: QueryBus, loggingInterceptor: LoggingInterceptor<Message<*>>) {
    queryBus.registerDispatchInterceptor(loggingInterceptor)
    queryBus.registerHandlerInterceptor(loggingInterceptor)
  }

  @Bean
  @Profile("command")
  fun giftCardCache(): Cache {
    return WeakReferenceCache()
  }
}
