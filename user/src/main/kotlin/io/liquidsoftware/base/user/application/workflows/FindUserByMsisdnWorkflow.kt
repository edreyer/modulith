package io.liquidsoftware.base.user.application.workflows

import io.liquidsoftware.base.user.application.port.`in`.FindUserByMsisdnQuery
import io.liquidsoftware.base.user.application.port.`in`.UserFoundEvent
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.application.workflows.mapper.toUserDto
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
internal class FindUserByMsisdnWorkflow(
  private val findUserPort: FindUserPort
) : BaseSafeWorkflow<FindUserByMsisdnQuery, UserFoundEvent>() {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerQueryHandler(this)

  override suspend fun execute(request: FindUserByMsisdnQuery): UserFoundEvent =
    findUserPort.findUserByMsisdn(request.msisdn)
      ?.let { UserFoundEvent(it.toUserDto()) }
      ?: throw UserNotFoundError(request.msisdn)

}
