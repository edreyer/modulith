package io.liquidsoftware.base.user.application.workflows

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import io.liquidsoftware.base.user.application.config.UserBridgekeeperConfig
import io.liquidsoftware.base.user.application.context.UserContext
import io.liquidsoftware.base.user.application.port.`in`.FindUserByMsisdnQuery
import io.liquidsoftware.base.user.application.port.`in`.UserFoundEvent
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.application.workflows.mapper.toUserDto
import io.liquidsoftware.base.user.bridgekeeper.UserSecured
import io.liquidsoftware.bridgekeeper.BridgeKeeper
import io.liquidsoftware.common.workflow.BaseSafeSecureWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import javax.annotation.PostConstruct

@Component
internal class FindUserByMsisdnWorkflow(
  private val findUserPort: FindUserPort,
  private val uc: UserContext,
  @Qualifier(UserBridgekeeperConfig.USER_BRIDGE_KEEPER) override val bk: BridgeKeeper
) : BaseSafeSecureWorkflow<FindUserByMsisdnQuery, UserFoundEvent>(),
  UserSecured<FindUserByMsisdnQuery> {

  override val ec = uc.ec

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerQueryHandler(this)

  override suspend fun userMatchingFn(request: FindUserByMsisdnQuery): Boolean =
    request.msisdn == uc.getCurrentUser().msisdn

  override suspend fun execute(request: FindUserByMsisdnQuery): UserFoundEvent =
    findUserPort.findUserByMsisdn(request.msisdn)
      ?.let { UserFoundEvent(it.toUserDto()) }
      ?: throw UserNotFoundError(request.msisdn)

}
