package ventures.dvx.base.user.application.workflows

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import ventures.dvx.base.user.application.config.UserBridgekeeperConfig.Companion.USER_BRIDGE_KEEPER
import ventures.dvx.base.user.application.context.UserContext
import ventures.dvx.base.user.application.port.`in`.FindUserByEmailQuery
import ventures.dvx.base.user.application.port.`in`.FindUserByEmailWorkflow
import ventures.dvx.base.user.application.port.`in`.FindUserEvent
import ventures.dvx.base.user.application.port.`in`.UserNotFoundError
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.workflows.mapper.toUserDto
import ventures.dvx.bridgekeeper.BridgeKeeper
import ventures.dvx.common.workflow.WorkflowDispatcher
import javax.annotation.PostConstruct

@Component
internal class FindUserByEmailWorkflowImpl(
  private val findUserPort: FindUserPort,
  override val uc: UserContext,
  @Qualifier(USER_BRIDGE_KEEPER) override val bk: BridgeKeeper
) : FindUserByEmailWorkflow() {

  @PostConstruct
  fun registerWithDispatcher() {
    WorkflowDispatcher.registerQueryHandler(this)
  }

  override suspend fun userMatchingFn(request: FindUserByEmailQuery): Boolean =
    request.email == uc.getCurrentUser().email

  override suspend fun execute(request: FindUserByEmailQuery): FindUserEvent =
    findUserPort.findUserByEmail(request.email)
      ?.let { FindUserEvent(it.toUserDto()) }
      ?: throw UserNotFoundError(request.email)

}
