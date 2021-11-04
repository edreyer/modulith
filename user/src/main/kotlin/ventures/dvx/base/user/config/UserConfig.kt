package ventures.dvx.base.user.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder
import ventures.dvx.base.user.adapter.out.persistence.UserPersistenceAdapter
import ventures.dvx.base.user.adapter.out.persistence.UserRepository
import ventures.dvx.base.user.application.port.`in`.FindUserUseCase
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.port.out.SaveNewUserPort
import ventures.dvx.base.user.application.usecase.FindUserUseCaseImpl
import ventures.dvx.base.user.application.usecase.RegisterUserWorkflowImpl

@Configuration
class UserConfig {

  @Bean
  fun userPersistenceAdapter(userRepository: UserRepository) : UserPersistenceAdapter =
    UserPersistenceAdapter(userRepository)

  @Bean
  fun registerUserWorkflow(
    passwordEncoder: PasswordEncoder,
    findUserPort: FindUserPort,
    saveNewUserPort: SaveNewUserPort
  ) : RegisterUserWorkflowImpl = RegisterUserWorkflowImpl(passwordEncoder, findUserPort, saveNewUserPort)

  @Bean
  fun findUserUseCase(
    findUserPort: FindUserPort
  ) : FindUserUseCase = FindUserUseCaseImpl(findUserPort)
}
