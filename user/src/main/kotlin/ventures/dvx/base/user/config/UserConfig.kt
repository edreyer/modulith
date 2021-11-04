package ventures.dvx.base.user.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder
import ventures.dvx.base.user.adapter.out.persistence.UserPersistenceAdapter
import ventures.dvx.base.user.adapter.out.persistence.UserRepository
import ventures.dvx.base.user.application.port.`in`.FindUserWorkflow
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.port.out.SaveNewUserPort
import ventures.dvx.base.user.application.workflows.FindUserWorkflowImpl
import ventures.dvx.base.user.application.workflows.RegisterUserWorkflowImpl
import ventures.dvx.common.workflow.RequestDispatcher

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
  ) : RegisterUserWorkflowImpl {
    val wf = RegisterUserWorkflowImpl(passwordEncoder, findUserPort, saveNewUserPort)
    RequestDispatcher.registerCommandHandler(wf)
    return wf
  }

  @Bean
  fun findUserWorkflow(
    findUserPort: FindUserPort
  ) : FindUserWorkflow {
    val wf = FindUserWorkflowImpl(findUserPort)
    RequestDispatcher.registerQueryHandler(wf)
    return wf
  }
}
