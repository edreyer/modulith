package io.liquidsoftware.base.booking.application.port.`in`

import arrow.core.Either
import io.liquidsoftware.common.application.error.ApplicationError

interface AvailabilityApi {
  suspend fun getAvailability(query: GetAvailabilityQuery): Either<ApplicationError, AvailabilityRetrievedEvent>
}
