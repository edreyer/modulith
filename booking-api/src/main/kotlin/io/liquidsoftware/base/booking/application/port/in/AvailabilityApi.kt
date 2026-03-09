package io.liquidsoftware.base.booking.application.port.`in`

import arrow.core.Either
import io.liquidsoftware.common.workflow.WorkflowError

interface AvailabilityApi {
  suspend fun getAvailability(query: GetAvailabilityQuery): Either<WorkflowError, AvailabilityRetrievedEvent>
}
