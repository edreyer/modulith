package io.liquidsoftware.base.server.config

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig(objectMapper: ObjectMapper) {
  init {
    objectMapper
      .registerKotlinModule()
      .setSerializationInclusion(NON_NULL)
  }
}
