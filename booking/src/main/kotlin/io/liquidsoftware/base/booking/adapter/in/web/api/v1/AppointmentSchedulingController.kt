package io.liquidsoftware.base.booking.adapter.`in`.web.api.v1

import io.liquidsoftware.base.booking.adapter.`in`.web.V1BookingPaths
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCancelledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDtoIn
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDtoOut
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentScheduledEvent
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentError
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentError
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.web.ControllerSupport
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

sealed class ScheduledAppointmentOutputDto
data class ScheduleSuccessDto(val appointment: AppointmentDtoOut) : ScheduledAppointmentOutputDto()
data class ScheduleErrorDto(val error: String) : ScheduledAppointmentOutputDto()

sealed class CancelAppointmentOutputDto
data class CancelApptSuccessDto(val appt: AppointmentDtoOut) : CancelAppointmentOutputDto()
data class CancelApptErrorDto(val error: String) : CancelAppointmentOutputDto()

@RestController
class AppointmentSchedulingController(
  private val ex: ExecutionContext
) : ControllerSupport {

  @PostMapping(value = [V1BookingPaths.SCHEDULE_APPT])
  suspend fun schedule(@RequestBody appt: AppointmentDtoIn)
    : ResponseEntity<ScheduledAppointmentOutputDto> {

    return WorkflowDispatcher.dispatch<AppointmentScheduledEvent>(
      ScheduleAppointmentCommand(ex.getCurrentUser().id, appt.scheduledTime, appt.duration, appt.workOrder)
    )
      .throwIfSpringError()
      .fold(
        { ResponseEntity.ok(ScheduleSuccessDto(it.appointmentDto)) },
        {
          when (it) {
            is ScheduleAppointmentError -> ResponseEntity.badRequest()
              .body(ScheduleErrorDto("Unexpected Error: ${it.error}"))
            else -> ResponseEntity.internalServerError()
              .body(ScheduleErrorDto("Unknown Error: ${it.message}"))
          }
        }
      )

  }

  @PostMapping(value = [V1BookingPaths.CANCEL_APPT])
  suspend fun cancel(@PathVariable appointmentId: String)
    : ResponseEntity<CancelAppointmentOutputDto> =
    WorkflowDispatcher.dispatch<AppointmentCancelledEvent>(CancelAppointmentCommand(appointmentId))
      .throwIfSpringError()
      .fold(
        { ResponseEntity.ok(CancelApptSuccessDto(it.appointmentDto)) },
        {
          when (it) {
            is CancelAppointmentError -> ResponseEntity.badRequest()
              .body(CancelApptErrorDto("Unexpected Error: ${it.error}"))
            else -> ResponseEntity.internalServerError()
              .body(CancelApptErrorDto("Unknown Error: ${it.message}"))
          }
        }
      )

}
