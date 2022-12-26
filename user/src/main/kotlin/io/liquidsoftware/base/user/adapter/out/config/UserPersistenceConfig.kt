package io.liquidsoftware.base.user.adapter.out.config

import io.liquidsoftware.base.user.adapter.out.persistence.UserPersistenceAdapter
import io.liquidsoftware.base.user.adapter.out.persistence.UserRepository
import io.liquidsoftware.common.persistence.AuditorAwareImpl
import io.liquidsoftware.common.security.acl.AclChecker
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.ReactiveAuditorAware
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories


@Configuration
@EnableReactiveMongoRepositories("io.liquidsoftware.base.user")
@EnableReactiveMongoAuditing
internal class UserPersistenceConfig {

  @Bean
  fun userPersistenceAdapter(
    userRepository: UserRepository,
    ac: AclChecker
  ): UserPersistenceAdapter = UserPersistenceAdapter(userRepository, ac)

  @Bean
  fun auditorAware(): ReactiveAuditorAware<String> = AuditorAwareImpl()

}
