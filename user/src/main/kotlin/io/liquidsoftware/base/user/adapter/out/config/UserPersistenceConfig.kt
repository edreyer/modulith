package io.liquidsoftware.base.user.adapter.out.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement
import io.liquidsoftware.base.user.adapter.out.persistence.UserPersistenceAdapter
import io.liquidsoftware.base.user.adapter.out.persistence.UserRepository

@Configuration
@EnableTransactionManagement
<<<<<<<< HEAD:user/src/main/kotlin/io/liquidsoftware/base/user/adapter/out/config/PersistenceConfig.kt
@EnableJpaRepositories("io.liquidsoftware.base.user")
internal  class PersistenceConfig {
========
@EnableJpaRepositories("ventures.dvx.base.user")
internal  class UserPersistenceConfig {
>>>>>>>> 84f8ed5 (Example of new Bounded Context):user/src/main/kotlin/io/liquidsoftware/base/user/adapter/out/config/UserPersistenceConfig.kt

  @Bean
   fun userPersistenceAdapter(userRepository: UserRepository) : UserPersistenceAdapter =
    UserPersistenceAdapter(userRepository)

}
