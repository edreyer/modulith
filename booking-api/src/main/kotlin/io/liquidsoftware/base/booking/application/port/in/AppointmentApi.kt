package io.liquidsoftware.base.booking.application.port.`in`

import io.liquidsoftware.common.workflow.Command
import io.liquidsoftware.common.workflow.Event
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

data class CancelAppointmentCommand(
  val appointmentId: String
) : Command

// events
sealed interface AppointmentEvent {
  val appointmentDto: AppointmentDtoOut
}
data class AppointmentScheduledEvent(override val appointmentDto: AppointmentDtoOut) : Event(),
  AppointmentEvent
data class AppointmentCancelledEvent(override val appointmentDto: AppointmentDtoOut) : Event(),
  AppointmentEvent

// Errors
sealed class ScheduleAppointmentError : RuntimeException() {
  abstract val error: String
}

data class DateTimeUnavailableError(override val error: String): ScheduleAppointmentError()
@ResponseStatus(code = HttpStatus.PRECONDITION_FAILED)
data class AppointmentValidationError(override val error: String) : ScheduleAppointmentError()
@ResponseStatus(code = HttpStatus.PRECONDITION_FAILED)
data class CancelAppointmentError(override val error: String) : ScheduleAppointmentError()

