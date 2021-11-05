package ventures.dvx.base.user.application.workflows

import arrow.core.computations.result
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import ventures.dvx.base.user.application.port.`in`.RegisterUserCommand
import ventures.dvx.base.user.application.port.`in`.RegisterUserError.UserExistsError
import ventures.dvx.base.user.application.port.`in`.RegisterUserEvent
import ventures.dvx.base.user.application.port.`in`.RegisterUserEvent.ValidUserRegistration
import ventures.dvx.base.user.application.port.`in`.RegisterUserWorkflow
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.port.out.SaveNewUserPort
import ventures.dvx.base.user.domain.UnregisteredUser
import ventures.dvx.common.ext.toResult
import ventures.dvx.common.workflow.WorkflowDispatcher
import javax.annotation.PostConstruct

@Component
class RegisterUserWorkflowImpl(
  private val passwordEncoder: PasswordEncoder,
  private val findUserPort: FindUserPort,
  private val saveNewUserPort: SaveNewUserPort
) : RegisterUserWorkflow {

  @PostConstruct
  fun registerWithDispatcher() {
    WorkflowDispatcher.registerCommandHandler(this)
  }

  override suspend operator fun invoke(request: RegisterUserCommand) : Result<RegisterUserEvent> =
    result {
      val unregisteredUser = validateNewUser(request).bind()
      val savedUser = saveNewUserPort.saveNewUser(unregisteredUser)
      ValidUserRegistration(savedUser.msisdn.value, savedUser.email.value)
    }

  private fun validateNewUser(cmd: RegisterUserCommand) : Result<UnregisteredUser> =
    findUserPort.findUserByEmail(cmd.email)
      ?.let { Result.failure(UserExistsError("User ${cmd.msisdn} exists")) }
      ?: UnregisteredUser.of(
        msisdn = cmd.msisdn,
        email = cmd.email,
        encryptedPassword = passwordEncoder.encode(cmd.password)
      )
        .toResult()

}
