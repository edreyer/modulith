package ventures.dvx.base.user.application.workflows

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import ventures.dvx.base.user.application.config.UserBridgekeeperConfig
import ventures.dvx.base.user.application.context.UserContext
import ventures.dvx.base.user.application.port.`in`.DisableUserCommand
import ventures.dvx.base.user.application.port.`in`.UserDisabledEvent
import ventures.dvx.base.user.application.port.`in`.UserNotFoundError
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.workflows.mapper.toUserDto
import ventures.dvx.bridgekeeper.BridgeKeeper
import ventures.dvx.common.events.EventPublisher
import ventures.dvx.common.workflow.BaseSafeSecureWorkflow
import ventures.dvx.common.workflow.WorkflowDispatcher
import javax.annotation.PostConstruct

@Component
internal class DisableUserWorkflow(
  private val findUserPort: FindUserPort,
  private val eventPublisher: EventPublisher,
  override val uc: UserContext,
  @Qualifier(UserBridgekeeperConfig.USER_BRIDGE_KEEPER) override val bk: BridgeKeeper
) : BaseSafeSecureWorkflow<DisableUserCommand, UserDisabledEvent>(),
  UserSecured<DisableUserCommand> {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  override suspend fun userMatchingFn(request: DisableUserCommand) =
    uc.getCurrentUser().id == request.userId

  override suspend fun execute(request: DisableUserCommand): UserDisabledEvent =
    findUserPort.findUserById(request.userId)
      ?.let { eventPublisher.publish(UserDisabledEvent(it.toUserDto())) }
      ?: throw UserNotFoundError("User not found with ID ${request.userId}")

}
