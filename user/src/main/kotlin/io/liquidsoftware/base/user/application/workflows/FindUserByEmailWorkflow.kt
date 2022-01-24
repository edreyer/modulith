package io.liquidsoftware.base.user.application.workflows

import io.liquidsoftware.base.user.application.config.UserBridgekeeperConfig.Companion.USER_BRIDGE_KEEPER
import io.liquidsoftware.base.user.application.context.UserContext
import io.liquidsoftware.base.user.application.port.`in`.FindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.UserFoundEvent
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.application.workflows.mapper.toUserDto
import io.liquidsoftware.base.user.bridgekeeper.UserSecured
import io.liquidsoftware.bridgekeeper.BridgeKeeper
import io.liquidsoftware.common.workflow.BaseSafeSecureWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
internal class FindUserByEmailWorkflow(
  private val findUserPort: FindUserPort,
  private val uc: UserContext,
  @Qualifier(USER_BRIDGE_KEEPER) override val bk: BridgeKeeper
) : BaseSafeSecureWorkflow<FindUserByEmailQuery, UserFoundEvent>(),
  UserSecured<FindUserByEmailQuery> {

  override val ec = uc.ec

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerQueryHandler(this)

  override suspend fun userMatchingFn(request: FindUserByEmailQuery): Boolean =
    request.email == uc.getCurrentUser().email

  override suspend fun execute(request: FindUserByEmailQuery): UserFoundEvent =
    findUserPort.findUserByEmail(request.email)
      ?.let { UserFoundEvent(it.toUserDto()) }
      ?: throw UserNotFoundError(request.email)

}
