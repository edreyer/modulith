package io.liquidsoftware.base.booking.application.port.`in`

import io.liquidsoftware.common.workflow.Command
import io.liquidsoftware.common.workflow.Event
import io.liquidsoftware.common.workflow.Query
import io.liquidsoftware.common.workflow.WorkflowError
import kotlinx.coroutines.flow.Flow
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.time.LocalDateTime

// Input

data class ScheduleAppointmentCommand(
  val userId: String,
  val scheduledTime: LocalDateTime,
  val duration: Long,
  val workOrder: WorkOrderDtoIn
) : Command

data class StartAppointmentCommand(
  val appointmentId: String
) : Command

data class CompleteAppointmentCommand(
  val appointmentId: String,
  val notes: String?
) : Command

data class CancelAppointmentCommand(
  val appointmentId: String,
  val notes: String?
) : Command

data class PayAppointmentCommand(
  val appointmentId: String,
  val paymentMethodId: String,
) : Command

data class FetchUserAppointmentsQuery(val userId: String) : Query

// events
sealed interface AppointmentEvent {
  val appointmentDto: AppointmentDtoOut
}
data class AppointmentScheduledEvent(override val appointmentDto: AppointmentDtoOut) : Event(),
  AppointmentEvent

data class AppointmentStartedEvent(override val appointmentDto: AppointmentDtoOut) : Event(),
  AppointmentEvent

data class AppointmentCompletedEvent(override val appointmentDto: AppointmentDtoOut) : Event(),
  AppointmentEvent

data class AppointmentCancelledEvent(override val appointmentDto: AppointmentDtoOut) : Event(),
  AppointmentEvent

data class AppointmentPaidEvent(override val appointmentDto: AppointmentDtoOut) : Event(),
  AppointmentEvent

data class UserAppointmentsFetchedEvent(val appointments: Flow<AppointmentDtoOut>) : Event()

// Errors
sealed class AppointmentError(override val message: String) : WorkflowError(message)

data class DateTimeUnavailableError(override val message: String): AppointmentError(message)

@ResponseStatus(code = HttpStatus.NOT_FOUND)
data class AppointmentNotFoundError(override val message: String) : AppointmentError(message)

@ResponseStatus(code = HttpStatus.PRECONDITION_FAILED)
data class AppointmentValidationError(override val message: String) : AppointmentError(message)

@ResponseStatus(code = HttpStatus.PRECONDITION_FAILED)
data class CancelAppointmentError(override val message: String) : AppointmentError(message)
