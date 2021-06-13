package ventures.dvx.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.axonframework.commandhandling.CommandBus
import org.axonframework.config.EventProcessingConfigurer
import org.axonframework.eventhandling.EventBus
import org.axonframework.messaging.Message
import org.axonframework.messaging.interceptors.LoggingInterceptor
import org.axonframework.queryhandling.QueryBus
import org.axonframework.serialization.Serializer
import org.axonframework.serialization.json.JacksonSerializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


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
    eventProcessingConfigurer.registerDefaultHandlerInterceptor {
        _: org.axonframework.config.Configuration, _: String -> loggingInterceptor
    }
  }

  @Autowired
  fun configureLoggingInterceptorFor(queryBus: QueryBus, loggingInterceptor: LoggingInterceptor<Message<*>>) {
    queryBus.registerDispatchInterceptor(loggingInterceptor)
    queryBus.registerHandlerInterceptor(loggingInterceptor)
  }

  @Bean
  @Qualifier(value = "messageSerializer")
  fun messageSerializer(): Serializer {
    val mapper = ObjectMapper()
    mapper.registerModule(JavaTimeModule())
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
    mapper.registerModule(Jdk8Module())
    mapper.registerModule(KotlinModule())
    return JacksonSerializer.builder()
      .objectMapper(mapper)
      .lenientDeserialization()
      .build()
  }

}
