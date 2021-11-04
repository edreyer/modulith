package ventures.dvx.base.user.application.workflows

import arrow.core.computations.result
import ventures.dvx.base.user.application.port.`in`.FindUserByEmailQuery
import ventures.dvx.base.user.application.port.`in`.FindUserEvent
import ventures.dvx.base.user.application.port.`in`.FindUserWorkflow
import ventures.dvx.base.user.application.port.`in`.UserNotFoundError
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.workflows.mapper.toUserDto

class FindUserWorkflowImpl(
  private val findUserPort: FindUserPort
) : FindUserWorkflow {

  override suspend operator fun invoke(request: FindUserByEmailQuery): Result<FindUserEvent> =
    result {
      findUserPort.findUserByEmail(request.email)
        ?.let { FindUserEvent(it.toUserDto()) }
        ?: throw UserNotFoundError(request.email)
    }

}
