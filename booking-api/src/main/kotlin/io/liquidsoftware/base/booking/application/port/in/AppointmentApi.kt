package io.liquidsoftware.base.booking.application.port.`in`

import arrow.core.Either
import io.liquidsoftware.common.workflow.WorkflowError

interface AppointmentApi {
  suspend fun scheduleAppointment(command: ScheduleAppointmentCommand): Either<WorkflowError, AppointmentScheduledEvent>
  suspend fun startAppointment(command: StartAppointmentCommand): Either<WorkflowError, AppointmentStartedEvent>
  suspend fun completeAppointment(command: CompleteAppointmentCommand): Either<WorkflowError, AppointmentCompletedEvent>
  suspend fun cancelAppointment(command: CancelAppointmentCommand): Either<WorkflowError, AppointmentCancelledEvent>
  suspend fun payAppointment(command: PayAppointmentCommand): Either<WorkflowError, AppointmentPaidEvent>
  suspend fun fetchUserAppointments(query: FetchUserAppointmentsQuery): Either<WorkflowError, UserAppointmentsFetchedEvent>
}
