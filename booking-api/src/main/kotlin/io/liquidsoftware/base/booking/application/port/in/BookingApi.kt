package io.liquidsoftware.base.booking.application.port.`in`

import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.application.error.NotFoundApplicationError
import io.liquidsoftware.common.application.error.ValidationApplicationError
import io.liquidsoftware.common.usecase.AppEvent
import io.liquidsoftware.common.usecase.Command
import io.liquidsoftware.common.usecase.Query
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

data class FetchUserAppointmentsQuery(val userId: String, val page: Int, val size: Int) : Query

// events
sealed interface AppointmentEvent {
  val appointmentDto: AppointmentDtoOut
}
data class AppointmentScheduledEvent(override val appointmentDto: AppointmentDtoOut) : AppEvent(),
  AppointmentEvent

data class AppointmentStartedEvent(override val appointmentDto: AppointmentDtoOut) : AppEvent(),
  AppointmentEvent

data class AppointmentCompletedEvent(override val appointmentDto: AppointmentDtoOut) : AppEvent(),
  AppointmentEvent

data class AppointmentCancelledEvent(override val appointmentDto: AppointmentDtoOut) : AppEvent(),
  AppointmentEvent

data class AppointmentPaidEvent(override val appointmentDto: AppointmentDtoOut) : AppEvent(),
  AppointmentEvent

data class UserAppointmentsFetchedEvent(val appointments: List<AppointmentDtoOut>) : AppEvent()

// Errors
sealed interface AppointmentError : ApplicationError

data class DateTimeUnavailableError(
  override val message: String,
) : AppointmentError, ValidationApplicationError {
  override val code: String = "DATE_TIME_UNAVAILABLE"
  override val metadata: Map<String, String> = emptyMap()
}

data class DateInPastError(
  override val message: String,
) : AppointmentError, ValidationApplicationError {
  override val code: String = "DATE_IN_PAST"
  override val metadata: Map<String, String> = emptyMap()
}

@ResponseStatus(code = HttpStatus.NOT_FOUND)
data class AppointmentNotFoundError(
  override val message: String,
) : AppointmentError, NotFoundApplicationError {
  override val code: String = "APPOINTMENT_NOT_FOUND"
  override val metadata: Map<String, String> = emptyMap()
}

@ResponseStatus(code = HttpStatus.PRECONDITION_FAILED)
data class AppointmentValidationError(
  override val message: String,
) : AppointmentError, ValidationApplicationError {
  override val code: String = "APPOINTMENT_VALIDATION_ERROR"
  override val metadata: Map<String, String> = emptyMap()
}

@ResponseStatus(code = HttpStatus.PRECONDITION_FAILED)
data class CancelAppointmentError(
  override val message: String,
) : AppointmentError, ValidationApplicationError {
  override val code: String = "CANCEL_APPOINTMENT_ERROR"
  override val metadata: Map<String, String> = emptyMap()
}
