package io.liquidsoftware.base.booking.application.port.out

import arrow.core.Either
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentEvent
import io.liquidsoftware.common.workflow.WorkflowError

interface AppointmentEventPort {
  suspend fun <T: AppointmentEvent> handle(event: T): Either<WorkflowError, T>
}
