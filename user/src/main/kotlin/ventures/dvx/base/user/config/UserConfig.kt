package ventures.dvx.base.user.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ventures.dvx.base.user.adapter.out.persistence.InMemoryUserRepository

@Configuration
class UserConfig {

  @Bean
  fun userRepository() = InMemoryUserRepository()

}
