package io.liquidsoftware.base.booking.adapter.`in`.web.api.v1

import arrow.core.getOrElse
import io.liquidsoftware.base.booking.adapter.`in`.web.V1BookingPaths
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCancelledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCompletedDtoIn
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCompletedEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDtoIn
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDtoOut
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentError
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentIdDtoIn
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentPaidEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentPaymentDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentScheduledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStartedEvent
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentError
import io.liquidsoftware.base.booking.application.port.`in`.CompleteAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.FetchUserAppointmentsQuery
import io.liquidsoftware.base.booking.application.port.`in`.PayAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.StartAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.UserAppointmentsFetchedEvent
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.web.ControllerSupport
import io.liquidsoftware.common.workflow.ServerError
import io.liquidsoftware.common.workflow.WorkflowDispatcher
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
  private val dispatcher: WorkflowDispatcher
) : ControllerSupport {

  private val log by LoggerDelegate()

  @PostMapping(value = [V1BookingPaths.SCHEDULE_APPT])
  suspend fun schedule(@RequestBody appt: AppointmentDtoIn)
    : ResponseEntity<ScheduledAppointmentOutputDto> {
    log.debug("AppointmentController.schedule()")
    return dispatcher.dispatch<AppointmentScheduledEvent>(
      ScheduleAppointmentCommand(ec.getCurrentUser().id, appt.scheduledTime, appt.duration, appt.workOrder)
    )
      .throwIfSpringError()
      .fold(
        {
          when (it) {
            is AppointmentError -> ResponseEntity.badRequest()
              .body(ScheduleErrorDto("Unexpected Error: ${it.message}"))
            else -> ResponseEntity.internalServerError()
              .body(ScheduleErrorDto("Unknown Error: ${it.message}"))
          }
        },
        { ResponseEntity.ok(ScheduleSuccessDto(it.appointmentDto)) }
      )
  }

  @PostMapping(value = [V1BookingPaths.IN_PROGRESS_APPT])
  suspend fun inProgress(@RequestBody appt: AppointmentIdDtoIn)
    : ResponseEntity<StartedAppointmentOutputDto> =
    dispatcher.dispatch<AppointmentStartedEvent>(
      StartAppointmentCommand(appt.id)
    )
      .throwIfSpringError()
      .fold(
        {
          when (it) {
            is AppointmentError -> ResponseEntity.badRequest()
              .body(StartedErrorDto("Unexpected Error: ${it.message}"))
            else -> ResponseEntity.internalServerError()
              .body(StartedErrorDto("Unknown Error: ${it.message}"))
          }
        },
        { ResponseEntity.ok(StartedSuccessDto(it.appointmentDto)) }
      )

  @PostMapping(value = [V1BookingPaths.COMPLETE_APPT])
  suspend fun complete(@RequestBody appt: AppointmentCompletedDtoIn)
    : ResponseEntity<CompletedAppointmentOutputDto> =
    dispatcher.dispatch<AppointmentCompletedEvent>(
      CompleteAppointmentCommand(appt.id, appt.notes)
    )
      .throwIfSpringError()
      .fold(
        {
          when (it) {
            is AppointmentError -> ResponseEntity.badRequest()
              .body(CompletedErrorDto("Unexpected Error: ${it.message}"))
            else -> ResponseEntity.internalServerError()
              .body(CompletedErrorDto("Unknown Error: ${it.message}"))
          }
        },
        { ResponseEntity.ok(CompletedSuccessDto(it.appointmentDto)) }
      )

  @PostMapping(value = [V1BookingPaths.PAY_APPT])
  suspend fun pay(@RequestBody request: AppointmentPaymentDto)
    : ResponseEntity<PaidAppointmentOutputDto> =
    dispatcher.dispatch<AppointmentPaidEvent>(
      PayAppointmentCommand(request.id, request.paymentMethodId)
    )
      .throwIfSpringError()
      .fold(
        {
          when (it) {
            is AppointmentError -> ResponseEntity.badRequest()
              .body(PaymentErrorDto("Unexpected Error: ${it.message}"))
            else -> ResponseEntity.internalServerError()
              .body(PaymentErrorDto("Unknown Error: ${it.message}"))
          }
        },
        { ResponseEntity.ok(PaymentSuccessDto(it.appointmentDto)) }
      )


  @PostMapping(value = [V1BookingPaths.CANCEL_APPT])
  suspend fun cancel(@RequestBody appt: AppointmentDtoIn)
    : ResponseEntity<CancelAppointmentOutputDto> =
    dispatcher.dispatch<AppointmentCancelledEvent>(
      CancelAppointmentCommand(appt.id!!, appt.workOrder.notes)
    )
      .throwIfSpringError()
      .fold(
        {
          when (it) {
            is CancelAppointmentError -> ResponseEntity.badRequest()
              .body(CancelApptErrorDto("Unexpected Error: ${it.message}"))
            else -> ResponseEntity.internalServerError()
              .body(CancelApptErrorDto("Unknown Error: ${it.message}"))
          }
        },
        { ResponseEntity.ok(CancelApptSuccessDto(it.appointmentDto)) }
      )

  @GetMapping(value = [V1BookingPaths.GET_USER_APPTS])
  suspend fun getUserAppointments(
    @RequestParam("page", required = false, defaultValue = "0") page: Int,
    @RequestParam("size", required = false, defaultValue = "20") size: Int): List<AppointmentDtoOut> {
    return dispatcher.dispatch<UserAppointmentsFetchedEvent>(
      FetchUserAppointmentsQuery(ec.getCurrentUser().id, page, size)
    )
      .throwIfSpringError()
      .map { it.appointments }
      .getOrElse { throw ServerError("Error fetching user appointments: ${it.message}") }
  }

}
