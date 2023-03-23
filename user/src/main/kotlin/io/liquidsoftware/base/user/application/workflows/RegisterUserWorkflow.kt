package io.liquidsoftware.base.user.application.workflows

import arrow.core.continuations.EffectScope
import arrow.core.continuations.effect
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
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowValidationError
import jakarta.annotation.PostConstruct
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
internal class RegisterUserWorkflow(
  private val passwordEncoder: PasswordEncoder,
  private val findUserPort: FindUserPort,
  private val userRegisteredPort: UserEventPort
) : BaseSafeWorkflow<RegisterUserCommand, UserRegisteredEvent>() {

  @PostConstruct
  fun registerWithDispatcher() {
    WorkflowDispatcher.registerCommandHandler(this)
  }

  context(EffectScope<WorkflowError>)
  override suspend fun execute(request: RegisterUserCommand) : UserRegisteredEvent {
    val user = validateNewUser(request)

    val result = runAsSuperUser {
      userRegisteredPort.handle(
        UserRegisteredEvent(user.toUserDto(), user.encryptedPassword.value)
      )
    }
    return result
  }

  context(EffectScope<WorkflowError>)
  private suspend fun validateNewUser(cmd: RegisterUserCommand) : UnregisteredUser =
    findUserPort.findUserByEmail(cmd.email)
      ?.let { shift(UserExistsError("User ${cmd.msisdn} exists")) }
      ?: effect { UnregisteredUser.of(
        msisdn = cmd.msisdn,
        email = cmd.email,
        encryptedPassword = passwordEncoder.encode(cmd.password),
        role = Role.valueOf(cmd.role)
      ) }.fold(
        { shift(WorkflowValidationError(it)) },
        { it }
      )

}
