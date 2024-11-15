package io.liquidsoftware.base.user.application.workflows

import arrow.core.raise.Raise
import io.liquidsoftware.base.user.application.mapper.toUserDto
import io.liquidsoftware.base.user.application.port.`in`.FindUserByMsisdnQuery
import io.liquidsoftware.base.user.application.port.`in`.UserFoundEvent
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowRegistry
import org.springframework.stereotype.Component

@Component
internal class FindUserByMsisdnWorkflow(
  private val findUserPort: FindUserPort
) : BaseSafeWorkflow<FindUserByMsisdnQuery, UserFoundEvent>() {

  override fun registerWithDispatcher() = WorkflowRegistry.registerQueryHandler(this)

  context(Raise<WorkflowError>)
  override suspend fun execute(request: FindUserByMsisdnQuery): UserFoundEvent =
    findUserPort.findUserByMsisdn(request.msisdn)
      ?.let { UserFoundEvent(it.toUserDto()) }
      ?: raise(UserNotFoundError(request.msisdn))

}
