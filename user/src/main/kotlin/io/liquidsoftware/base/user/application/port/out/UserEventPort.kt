package io.liquidsoftware.base.user.application.port.out

import arrow.core.Either
import io.liquidsoftware.base.user.application.port.`in`.UserEvent
import io.liquidsoftware.base.user.application.port.`in`.UserRegisteredEvent
import io.liquidsoftware.common.workflow.WorkflowError

interface UserEventPort {

  suspend fun handle(event: UserRegisteredEvent): Either<WorkflowError, UserRegisteredEvent>
  suspend fun <T: UserEvent> handle(event: T): T

}
