package io.liquidsoftware.base.booking.adapter.`in`.web.api.v1

import arrow.core.getOrElse
import io.liquidsoftware.base.booking.adapter.`in`.web.V1BookingPaths
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentApi
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCompletedDtoIn
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDtoIn
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDtoOut
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentIdDtoIn
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentPaymentDto
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.CompleteAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.FetchUserAppointmentsQuery
import io.liquidsoftware.base.booking.application.port.`in`.PayAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.StartAppointmentCommand
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.application.error.NotFoundApplicationError
import io.liquidsoftware.common.application.error.ValidationApplicationError
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.workflow.ServerError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

sealed class ScheduledAppointmentOutputDto
data class ScheduleSuccessDto(val appointment: AppointmentDtoOut) : ScheduledAppointmentOutputDto()
data class ScheduleErrorDto(val error: String) : ScheduledAppointmentOutputDto()

sealed class StartedAppointmentOutputDto
data class StartedSuccessDto(val appointment: AppointmentDtoOut) : StartedAppointmentOutputDto()
data class StartedErrorDto(val error: String) : StartedAppointmentOutputDto()

sealed class CompletedAppointmentOutputDto
data class CompletedSuccessDto(val appointment: AppointmentDtoOut) : CompletedAppointmentOutputDto()
data class CompletedErrorDto(val error: String) : CompletedAppointmentOutputDto()

sealed class PaidAppointmentOutputDto
data class PaymentSuccessDto(val appointment: AppointmentDtoOut) : PaidAppointmentOutputDto()
data class PaymentErrorDto(val error: String) : PaidAppointmentOutputDto()

sealed class CancelAppointmentOutputDto
data class CancelApptSuccessDto(val appt: AppointmentDtoOut) : CancelAppointmentOutputDto()
data class CancelApptErrorDto(val error: String) : CancelAppointmentOutputDto()

@RestController
class AppointmentController(
  private val ec: ExecutionContext,
  private val appointmentApi: AppointmentApi,
) {

  private val log by LoggerDelegate()

  @PostMapping(value = [V1BookingPaths.SCHEDULE_APPT])
  suspend fun schedule(@RequestBody appt: AppointmentDtoIn)
    : ResponseEntity<ScheduledAppointmentOutputDto> {
    log.debug("AppointmentController.schedule()")
    return appointmentApi.scheduleAppointment(
      ScheduleAppointmentCommand(ec.getCurrentUser().id, appt.scheduledTime, appt.duration, appt.workOrder)
    )
      .fold(
        { it.toErrorResponse(::ScheduleErrorDto) },
        { ResponseEntity.ok(ScheduleSuccessDto(it.appointmentDto)) }
      )
  }

  @PostMapping(value = [V1BookingPaths.IN_PROGRESS_APPT])
  suspend fun inProgress(@RequestBody appt: AppointmentIdDtoIn)
    : ResponseEntity<StartedAppointmentOutputDto> =
    appointmentApi.startAppointment(
      StartAppointmentCommand(appt.id)
    )
      .fold(
        { it.toErrorResponse(::StartedErrorDto) },
        { ResponseEntity.ok(StartedSuccessDto(it.appointmentDto)) }
      )

  @PostMapping(value = [V1BookingPaths.COMPLETE_APPT])
  suspend fun complete(@RequestBody appt: AppointmentCompletedDtoIn)
    : ResponseEntity<CompletedAppointmentOutputDto> =
    appointmentApi.completeAppointment(
      CompleteAppointmentCommand(appt.id, appt.notes)
    )
      .fold(
        { it.toErrorResponse(::CompletedErrorDto) },
        { ResponseEntity.ok(CompletedSuccessDto(it.appointmentDto)) }
      )

  @PostMapping(value = [V1BookingPaths.PAY_APPT])
  suspend fun pay(@RequestBody request: AppointmentPaymentDto)
    : ResponseEntity<PaidAppointmentOutputDto> =
    appointmentApi.payAppointment(
      PayAppointmentCommand(request.id, request.paymentMethodId)
    )
      .fold(
        { it.toErrorResponse(::PaymentErrorDto) },
        { ResponseEntity.ok(PaymentSuccessDto(it.appointmentDto)) }
      )

  @PostMapping(value = [V1BookingPaths.CANCEL_APPT])
  suspend fun cancel(@RequestBody appt: AppointmentDtoIn)
    : ResponseEntity<CancelAppointmentOutputDto> =
    appointmentApi.cancelAppointment(
      CancelAppointmentCommand(appt.id!!, appt.workOrder.notes)
    )
      .fold(
        { it.toErrorResponse(::CancelApptErrorDto) },
        { ResponseEntity.ok(CancelApptSuccessDto(it.appointmentDto)) }
      )

  @GetMapping(value = [V1BookingPaths.GET_USER_APPTS])
  suspend fun getUserAppointments(
    @RequestParam("page", required = false, defaultValue = "0") page: Int,
    @RequestParam("size", required = false, defaultValue = "20") size: Int): List<AppointmentDtoOut> {
    return appointmentApi.fetchUserAppointments(
      FetchUserAppointmentsQuery(ec.getCurrentUser().id, page, size)
    )
      .map { it.appointments }
      .getOrElse { throw ServerError("Error fetching user appointments: ${it.message}") }
  }

}

private fun <T : Any> ApplicationError.toErrorResponse(errorBody: (String) -> T): ResponseEntity<T> {
  val status = when (this) {
    is NotFoundApplicationError -> HttpStatus.NOT_FOUND
    is ApplicationError.Unauthorized -> HttpStatus.UNAUTHORIZED
    is ApplicationError.Forbidden -> HttpStatus.FORBIDDEN
    is ValidationApplicationError -> HttpStatus.BAD_REQUEST
    else -> HttpStatus.INTERNAL_SERVER_ERROR
  }
  return ResponseEntity.status(status).body(errorBody(message))
}
