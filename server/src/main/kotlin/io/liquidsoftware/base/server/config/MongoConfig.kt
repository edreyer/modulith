package io.liquidsoftware.base.server.config

import io.liquidsoftware.common.persistence.AuditorAwareImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.mongodb.config.EnableMongoAuditing

@Configuration
@EnableMongoAuditing
class MongoConfig {
  @Bean
  fun auditorProvider(): AuditorAware<String> = AuditorAwareImpl()

}
