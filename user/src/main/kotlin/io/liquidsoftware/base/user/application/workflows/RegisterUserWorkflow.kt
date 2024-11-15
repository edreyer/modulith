package io.liquidsoftware.base.user.application.workflows


import arrow.core.raise.Raise
import arrow.core.raise.effect
import arrow.core.raise.fold
import io.liquidsoftware.base.user.application.mapper.toUserDto
import io.liquidsoftware.base.user.application.port.`in`.RegisterUserCommand
import io.liquidsoftware.base.user.application.port.`in`.UserExistsError
import io.liquidsoftware.base.user.application.port.`in`.UserRegisteredEvent
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.application.port.out.UserEventPort
import io.liquidsoftware.base.user.domain.Role
import io.liquidsoftware.base.user.domain.UnregisteredUser
import io.liquidsoftware.common.security.runAsSuperUser
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowRegistry
import io.liquidsoftware.common.workflow.WorkflowValidationError
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
internal class RegisterUserWorkflow(
  private val passwordEncoder: PasswordEncoder,
  private val findUserPort: FindUserPort,
  private val userRegisteredPort: UserEventPort
) : BaseSafeWorkflow<RegisterUserCommand, UserRegisteredEvent>() {

  override fun registerWithDispatcher() = WorkflowRegistry.registerCommandHandler(this)

  context(Raise<WorkflowError>)
  override suspend fun execute(request: RegisterUserCommand) : UserRegisteredEvent {

    val result = runAsSuperUser {
      val user = validateNewUser(request)
      userRegisteredPort.handle(
        UserRegisteredEvent(user.toUserDto(), user.encryptedPassword.value)
      )
    }
    return result
  }

  context(Raise<WorkflowError>)
  private suspend fun validateNewUser(cmd: RegisterUserCommand) : UnregisteredUser =
    findUserPort.findUserByEmail(cmd.email)
      ?.let { raise(UserExistsError("User ${cmd.msisdn} exists")) }
      ?: effect { UnregisteredUser.of(
        msisdn = cmd.msisdn,
        email = cmd.email,
        encryptedPassword = passwordEncoder.encode(cmd.password),
        role = Role.valueOf(cmd.role)
      ) }.fold(
        { raise(WorkflowValidationError(it)) },
        { it }
      )

}
