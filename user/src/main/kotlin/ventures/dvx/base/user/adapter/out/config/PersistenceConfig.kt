package ventures.dvx.base.user.adapter.out.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement
import ventures.dvx.base.user.adapter.out.persistence.UserPersistenceAdapter
import ventures.dvx.base.user.adapter.out.persistence.UserRepository

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories("ventures.dvx.base.user")
internal  class PersistenceConfig {

  @Bean
   fun userPersistenceAdapter(userRepository: UserRepository) : UserPersistenceAdapter =
    UserPersistenceAdapter(userRepository)

}
