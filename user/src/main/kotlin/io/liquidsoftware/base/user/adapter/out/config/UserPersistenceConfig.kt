package io.liquidsoftware.base.user.adapter.out.config

import io.liquidsoftware.base.user.adapter.out.persistence.UserPersistenceAdapter
import io.liquidsoftware.base.user.adapter.out.persistence.UserRepository
import io.liquidsoftware.common.security.acl.AclChecker
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories


@Configuration
@EnableMongoRepositories("io.liquidsoftware.base.user")
internal class UserPersistenceConfig {

  @Bean
  fun userPersistenceAdapter(
    userRepository: UserRepository,
    ac: AclChecker
  ): UserPersistenceAdapter = UserPersistenceAdapter(userRepository, ac)

}
