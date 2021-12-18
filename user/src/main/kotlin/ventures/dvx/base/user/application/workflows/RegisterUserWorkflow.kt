package ventures.dvx.base.user.application.workflows

import arrow.core.computations.ResultEffect.bind
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import ventures.dvx.base.user.application.port.`in`.RegisterUserCommand
import ventures.dvx.base.user.application.port.`in`.RegisterUserError.UserExistsError
import ventures.dvx.base.user.application.port.`in`.UserRegisteredEvent
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.workflows.mapper.toUserDto
import ventures.dvx.base.user.domain.Role
import ventures.dvx.base.user.domain.UnregisteredUser
import ventures.dvx.common.events.EventPublisher
import ventures.dvx.common.ext.toResult
import ventures.dvx.common.workflow.BaseSafeWorkflow
import ventures.dvx.common.workflow.WorkflowDispatcher
import javax.annotation.PostConstruct

@Component
internal class RegisterUserWorkflow(
  private val passwordEncoder: PasswordEncoder,
  private val findUserPort: FindUserPort,
  private val eventPublisher: EventPublisher
) : BaseSafeWorkflow<RegisterUserCommand, UserRegisteredEvent>() {

  @PostConstruct
  fun registerWithDispatcher() {
    WorkflowDispatcher.registerCommandHandler(this)
  }

  override suspend fun execute(request: RegisterUserCommand) : UserRegisteredEvent {
    val user = validateNewUser(request).bind()
    return eventPublisher.publish(
      UserRegisteredEvent(user.toUserDto(), user.encryptedPassword.value)
    )
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
