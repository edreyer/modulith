package io.liquidsoftware.base.booking.application.port.`in`

import arrow.core.Either
import io.liquidsoftware.common.application.error.ApplicationError

interface AppointmentApi {
  suspend fun scheduleAppointment(command: ScheduleAppointmentCommand): Either<ApplicationError, AppointmentScheduledEvent>
  suspend fun startAppointment(command: StartAppointmentCommand): Either<ApplicationError, AppointmentStartedEvent>
  suspend fun completeAppointment(command: CompleteAppointmentCommand): Either<ApplicationError, AppointmentCompletedEvent>
  suspend fun cancelAppointment(command: CancelAppointmentCommand): Either<ApplicationError, AppointmentCancelledEvent>
  suspend fun payAppointment(command: PayAppointmentCommand): Either<ApplicationError, AppointmentPaidEvent>
  suspend fun fetchUserAppointments(query: FetchUserAppointmentsQuery): Either<ApplicationError, UserAppointmentsFetchedEvent>
}
