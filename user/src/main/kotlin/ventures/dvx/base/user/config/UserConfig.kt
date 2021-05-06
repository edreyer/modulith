package ventures.dvx.base.user.config

import org.slf4j.Logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ventures.dvx.base.user.adapter.out.persistence.InMemoryUserRepository
import ventures.dvx.base.user.adapter.out.persistence.UserPersistenceAdapter
import ventures.dvx.base.user.adapter.out.persistence.UserRepository

@Configuration
class UserConfig {

  @Bean
  fun userRepository(): UserRepository = InMemoryUserRepository()

  @Bean
  fun userPersistenceAdapter(logger: Logger, userRepository: UserRepository) : UserPersistenceAdapter =
    UserPersistenceAdapter(logger, userRepository)

}
