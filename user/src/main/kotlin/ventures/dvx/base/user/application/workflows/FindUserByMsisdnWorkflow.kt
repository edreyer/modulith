package ventures.dvx.base.user.application.workflows

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import ventures.dvx.base.user.application.context.UserContext
import ventures.dvx.base.user.application.port.`in`.FindUserByMsisdnQuery
import ventures.dvx.base.user.application.port.`in`.UserFoundEvent
import ventures.dvx.base.user.application.port.`in`.UserNotFoundError
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.workflows.mapper.toUserDto
import ventures.dvx.base.user.config.UserBridgekeeperConfig
import ventures.dvx.bridgekeeper.BridgeKeeper
import ventures.dvx.common.workflow.BaseSafeSecureWorkflow
import ventures.dvx.common.workflow.WorkflowDispatcher
import javax.annotation.PostConstruct

@Component
internal class FindUserByMsisdnWorkflow(
  private val findUserPort: FindUserPort,
  override val uc: UserContext,
  @Qualifier(UserBridgekeeperConfig.USER_BRIDGE_KEEPER) override val bk: BridgeKeeper
) : BaseSafeSecureWorkflow<FindUserByMsisdnQuery, UserFoundEvent>(),
  UserSecured<FindUserByMsisdnQuery> {

  @PostConstruct
  fun registerWithDispatcher() {
    WorkflowDispatcher.registerQueryHandler(this)
  }

  override suspend fun userMatchingFn(request: FindUserByMsisdnQuery): Boolean =
    request.msisdn == uc.getCurrentUser().msisdn

  override suspend fun execute(request: FindUserByMsisdnQuery): UserFoundEvent =
    findUserPort.findUserByMsisdn(request.msisdn)
      ?.let { UserFoundEvent(it.toUserDto()) }
      ?: throw UserNotFoundError(request.msisdn)

}
