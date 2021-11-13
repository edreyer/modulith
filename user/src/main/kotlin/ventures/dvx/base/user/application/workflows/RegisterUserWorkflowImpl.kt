package ventures.dvx.base.user.application.workflows

import arrow.core.computations.ResultEffect.bind
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import ventures.dvx.base.user.application.config.UserBridgekeeperConfig
import ventures.dvx.base.user.application.port.`in`.RegisterUserCommand
import ventures.dvx.base.user.application.port.`in`.RegisterUserError.UserExistsError
import ventures.dvx.base.user.application.port.`in`.RegisterUserEvent
import ventures.dvx.base.user.application.port.`in`.RegisterUserEvent.ValidUserRegistration
import ventures.dvx.base.user.application.port.`in`.RegisterUserWorkflow
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.port.out.SaveNewUserPort
import ventures.dvx.base.user.application.workflows.mapper.toUserDto
import ventures.dvx.base.user.domain.UnregisteredUser
import ventures.dvx.bridgekeeper.BridgeKeeper
import ventures.dvx.common.ext.toResult
import ventures.dvx.common.workflow.WorkflowDispatcher
import javax.annotation.PostConstruct

@Component
internal class RegisterUserWorkflowImpl(
  private val passwordEncoder: PasswordEncoder,
  private val findUserPort: FindUserPort,
  private val saveNewUserPort: SaveNewUserPort,
  @Qualifier(UserBridgekeeperConfig.USER_BRIDGE_KEEPER) val bk: BridgeKeeper
) : RegisterUserWorkflow() {

  @PostConstruct
  fun registerWithDispatcher() {
    WorkflowDispatcher.registerCommandHandler(this)
  }

  override suspend fun execute(request: RegisterUserCommand) : RegisterUserEvent {
    val unregisteredUser = validateNewUser(request).bind()
    val savedUser = saveNewUserPort.saveNewUser(unregisteredUser)
    return ValidUserRegistration(savedUser.toUserDto())
  }

  private fun validateNewUser(cmd: RegisterUserCommand) : Result<UnregisteredUser> =
    findUserPort.findUserByEmail(cmd.email)
      ?.let { Result.failure(UserExistsError("User ${cmd.msisdn} exists")) }
      ?: UnregisteredUser.of(
        msisdn = cmd.msisdn,
        email = cmd.email,
        encryptedPassword = passwordEncoder.encode(cmd.password),
        role = cmd.role
      )
        .toResult()

}
