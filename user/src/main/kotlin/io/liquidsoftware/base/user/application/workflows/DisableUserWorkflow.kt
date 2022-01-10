package io.liquidsoftware.base.user.application.workflows

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import io.liquidsoftware.base.user.application.config.UserBridgekeeperConfig
import io.liquidsoftware.base.user.application.context.UserContext
import io.liquidsoftware.base.user.application.port.`in`.DisableUserCommand
import io.liquidsoftware.base.user.application.port.`in`.UserDisabledEvent
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.application.workflows.mapper.toUserDto
import io.liquidsoftware.base.user.bridgekeeper.UserSecured
import io.liquidsoftware.bridgekeeper.BridgeKeeper
import io.liquidsoftware.common.events.EventPublisher
import io.liquidsoftware.common.workflow.BaseSafeSecureWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import javax.annotation.PostConstruct

@Component
internal class DisableUserWorkflow(
  private val findUserPort: FindUserPort,
  private val eventPublisher: EventPublisher,
  private val uc: UserContext,
  @Qualifier(UserBridgekeeperConfig.USER_BRIDGE_KEEPER) override val bk: BridgeKeeper
) : BaseSafeSecureWorkflow<DisableUserCommand, UserDisabledEvent>(),
  UserSecured<DisableUserCommand> {

  override val ec = uc.ec

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  override suspend fun userMatchingFn(request: DisableUserCommand) =
    uc.getCurrentUser().id == request.userId

  override suspend fun execute(request: DisableUserCommand): UserDisabledEvent =
    findUserPort.findUserById(request.userId)
      ?.let { eventPublisher.publish(UserDisabledEvent(it.toUserDto())) }
      ?: throw UserNotFoundError("User not found with ID ${request.userId}")

}
