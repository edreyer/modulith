package ventures.dvx.base.user.application.workflows

import arrow.core.computations.result
import org.springframework.stereotype.Component
import ventures.dvx.base.user.application.port.`in`.FindUserByMsisdnQuery
import ventures.dvx.base.user.application.port.`in`.FindUserByMsisdnWorkflow
import ventures.dvx.base.user.application.port.`in`.FindUserEvent
import ventures.dvx.base.user.application.port.`in`.UserNotFoundError
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.workflows.mapper.toUserDto
import ventures.dvx.common.workflow.WorkflowDispatcher
import javax.annotation.PostConstruct

@Component
class FindUserByMsisdnWorkflowImpl(
  private val findUserPort: FindUserPort
) : FindUserByMsisdnWorkflow {

  @PostConstruct
  fun registerWithDispatcher() {
    WorkflowDispatcher.registerQueryHandler(this)
  }

  override suspend operator fun invoke(request: FindUserByMsisdnQuery): Result<FindUserEvent> =
    result {
      findUserPort.findUserByMsisdn(request.msisdn)
        ?.let { FindUserEvent(it.toUserDto()) }
        ?: throw UserNotFoundError(request.msisdn)
    }

}
