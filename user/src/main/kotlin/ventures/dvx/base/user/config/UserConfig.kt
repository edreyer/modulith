package ventures.dvx.base.user.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder
import ventures.dvx.base.user.adapter.out.persistence.InMemoryUserRepository
import ventures.dvx.base.user.adapter.out.persistence.UserPersistenceAdapter
import ventures.dvx.base.user.adapter.out.persistence.UserRepository
import ventures.dvx.base.user.application.port.`in`.RegisterUserUseCase
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.port.out.SaveNewUserPort
import ventures.dvx.base.user.application.usecase.RegisterUserUseCaseImpl

@Configuration
class UserConfig {

  @Bean
  fun userRepository(): UserRepository = InMemoryUserRepository()

  @Bean
  fun userPersistenceAdapter(userRepository: UserRepository) : UserPersistenceAdapter =
    UserPersistenceAdapter(userRepository)

  @Bean
  fun registerUserUseCase(
    passwordEncoder: PasswordEncoder,
    findUserPort: FindUserPort,
    saveNewUserPort: SaveNewUserPort
  ) : RegisterUserUseCase = RegisterUserUseCaseImpl(passwordEncoder, findUserPort, saveNewUserPort)
}
