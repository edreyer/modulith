package ventures.dvx.base.user.application.usecase

import arrow.core.computations.result
import org.springframework.security.crypto.password.PasswordEncoder
import ventures.dvx.base.user.application.port.`in`.RegisterUserCommand
import ventures.dvx.base.user.application.port.`in`.RegisterUserError.UserExistsError
import ventures.dvx.base.user.application.port.`in`.RegisterUserEvent
import ventures.dvx.base.user.application.port.`in`.RegisterUserWorkflow
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.port.out.SaveNewUserPort
import ventures.dvx.base.user.domain.UnregisteredUser
import ventures.dvx.common.ext.toResult

class RegisterUserWorkflowImpl(
  private val passwordEncoder: PasswordEncoder,
  private val findUserPort: FindUserPort,
  private val saveNewUserPort: SaveNewUserPort
) : RegisterUserWorkflow {

  override suspend operator fun invoke(request: RegisterUserCommand) : Result<RegisterUserEvent> =
    result {
      val unregisteredUser = validateNewUser(request).bind()
      val savedUser = saveNewUserPort.saveNewUser(unregisteredUser)
      RegisterUserEvent.ValidUserRegistration(savedUser.username.value, savedUser.email.value)
    }

  private fun validateNewUser(cmd: RegisterUserCommand) : Result<UnregisteredUser> =
    findUserPort.findUserByUsername(cmd.username)
      ?.let { Result.failure(UserExistsError("User ${cmd.username} exists")) }
      ?: UnregisteredUser.of(
        username = cmd.username,
        email = cmd.email,
        encryptedPassword = passwordEncoder.encode(cmd.password)
      )
        .toResult()

}
