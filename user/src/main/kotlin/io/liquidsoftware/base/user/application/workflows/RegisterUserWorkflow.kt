package io.liquidsoftware.base.user.application.workflows


import arrow.core.raise.Raise
import arrow.core.raise.either
import io.liquidsoftware.base.user.application.mapper.toUserDto
import io.liquidsoftware.base.user.application.port.`in`.RegisterUserCommand
import io.liquidsoftware.base.user.application.port.`in`.UserExistsError
import io.liquidsoftware.base.user.application.port.`in`.UserRegisteredEvent
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.application.port.out.UserEventPort
import io.liquidsoftware.base.user.domain.Role
import io.liquidsoftware.base.user.domain.UnregisteredUser
import arrow.core.raise.context.bind
import io.liquidsoftware.common.ext.bindValidation
import arrow.core.raise.context.raise
import io.liquidsoftware.common.security.runAsSuperUser
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowRegistry
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
internal class RegisterUserWorkflow(
  private val passwordEncoder: PasswordEncoder,
  private val findUserPort: FindUserPort,
  private val userRegisteredPort: UserEventPort
) : BaseSafeWorkflow<RegisterUserCommand, UserRegisteredEvent>() {

  override fun registerWithDispatcher() = WorkflowRegistry.registerCommandHandler(this)

  context(_: Raise<WorkflowError>)
  override suspend fun execute(request: RegisterUserCommand) : UserRegisteredEvent {

    val result = runAsSuperUser {
      val user = validateNewUser(request)
      userRegisteredPort.handle(
        UserRegisteredEvent(user.toUserDto(), user.encryptedPassword.value)
      ).bind()
    }
    return result
  }

  context(_: Raise<WorkflowError>)
  private suspend fun validateNewUser(cmd: RegisterUserCommand) : UnregisteredUser =
    findUserPort.findUserByEmail(cmd.email).bind()
      ?.let { raise(UserExistsError("User ${cmd.msisdn} exists")) }
      ?: either {
        val encodedPassword = checkNotNull(passwordEncoder.encode(cmd.password)) {
          "Password encoder returned null"
        }

        UnregisteredUser.of(
          msisdn = cmd.msisdn,
          email = cmd.email,
          encryptedPassword = encodedPassword,
          role = Role.valueOf(cmd.role)
        )
      }.bindValidation()

}
