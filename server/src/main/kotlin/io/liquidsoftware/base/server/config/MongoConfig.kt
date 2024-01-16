package io.liquidsoftware.base.server.config

import io.liquidsoftware.common.persistence.AuditorAwareImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.ReactiveAuditorAware
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing

@Configuration
@EnableReactiveMongoAuditing
class MongoConfig {
  @Bean
  fun auditorProvider(): ReactiveAuditorAware<String> = AuditorAwareImpl()

}
