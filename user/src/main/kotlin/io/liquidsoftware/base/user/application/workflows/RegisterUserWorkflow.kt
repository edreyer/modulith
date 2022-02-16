package io.liquidsoftware.base.user.application.workflows

import arrow.core.computations.ResultEffect.bind
import io.liquidsoftware.base.user.application.port.`in`.RegisterUserCommand
import io.liquidsoftware.base.user.application.port.`in`.RegisterUserError.UserExistsError
import io.liquidsoftware.base.user.application.port.`in`.UserRegisteredEvent
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.application.port.out.UserEventPort
import io.liquidsoftware.base.user.application.workflows.mapper.toUserDto
import io.liquidsoftware.base.user.domain.Role
import io.liquidsoftware.base.user.domain.UnregisteredUser
import io.liquidsoftware.common.ext.toResult
import io.liquidsoftware.common.security.runAsSuperUser
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

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

  override suspend fun execute(request: RegisterUserCommand) : UserRegisteredEvent {
    val user = validateNewUser(request).bind()
    return runAsSuperUser {
      userRegisteredPort.handle(
        UserRegisteredEvent(user.toUserDto(), user.encryptedPassword.value)
      )
    }
  }

  private suspend fun validateNewUser(cmd: RegisterUserCommand) : Result<UnregisteredUser> =
    findUserPort.findUserByEmail(cmd.email)
      ?.let { Result.failure(UserExistsError("User ${cmd.msisdn} exists")) }
      ?: UnregisteredUser.of(
        msisdn = cmd.msisdn,
        email = cmd.email,
        encryptedPassword = passwordEncoder.encode(cmd.password),
        role = Role.valueOf(cmd.role)
      )
        .toResult()

}
