package io.liquidsoftware.base.user.adapter.out.config

import io.liquidsoftware.base.user.adapter.out.persistence.UserPersistenceAdapter
import io.liquidsoftware.base.user.adapter.out.persistence.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories("io.liquidsoftware.base.user")
internal  class UserPersistenceConfig {

  @Bean
   fun userPersistenceAdapter(userRepository: UserRepository) : UserPersistenceAdapter =
    UserPersistenceAdapter(userRepository)

}
