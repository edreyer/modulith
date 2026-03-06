package io.liquidsoftware.base.server.config

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.annotation.JsonInclude.Value
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class JacksonConfig {
  @Bean
  fun objectMapper(): ObjectMapper =
    JsonMapper.builder()
      .findAndAddModules()
      .defaultPropertyInclusion(Value.construct(NON_NULL, NON_NULL))
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .build()
}
