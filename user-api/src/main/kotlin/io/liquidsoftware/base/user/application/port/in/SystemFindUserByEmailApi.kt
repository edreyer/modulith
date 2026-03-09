package io.liquidsoftware.base.user.application.port.`in`

import arrow.core.Either
import io.liquidsoftware.common.workflow.WorkflowError

interface SystemFindUserByEmailApi {
  suspend fun findSystemUserByEmail(query: SystemFindUserByEmailQuery): Either<WorkflowError, SystemUserFoundEvent>
}
