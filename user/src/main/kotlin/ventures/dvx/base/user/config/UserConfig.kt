package ventures.dvx.base.user.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ventures.dvx.base.user.adapter.out.persistence.UserPersistenceAdapter
import ventures.dvx.base.user.adapter.out.persistence.UserRepository

@Configuration
class UserConfig {

  @Bean
  fun userPersistenceAdapter(userRepository: UserRepository) : UserPersistenceAdapter =
    UserPersistenceAdapter(userRepository)

}
