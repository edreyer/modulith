package io.liquidsoftware.base.booking.adapter.`in`.web.api.v1

import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCancelledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.CancelledAppointmentDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.ScheduledAppointmentDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentScheduledEvent
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentError
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentError.CancelAppointmentError
import io.liquidsoftware.common.web.ControllerSupport
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

sealed class ScheduledAppointmentOutputDto
data class ScheduleSuccessDto(val appointment: ScheduledAppointmentDto) : io.liquidsoftware.base.booking.adapter.`in`.web.api.v1.ScheduledAppointmentOutputDto()
data class ScheduleErrorDto(val error: String) : io.liquidsoftware.base.booking.adapter.`in`.web.api.v1.ScheduledAppointmentOutputDto()

sealed class CancelAppointmentOutputDto
data class CancelApptSuccessDto(val appt: CancelledAppointmentDto) : io.liquidsoftware.base.booking.adapter.`in`.web.api.v1.CancelAppointmentOutputDto()
data class CancelApptErrorDto(val error: String) : io.liquidsoftware.base.booking.adapter.`in`.web.api.v1.CancelAppointmentOutputDto()

@RestController
class AppointmentSchedulingController : ControllerSupport {

  @PostMapping(value = [io.liquidsoftware.base.booking.adapter.`in`.web.V1BookingPaths.SCHEDULE_APPT])
  suspend fun schedule(@RequestBody draft: AppointmentDto.DraftAppointmentDto)
    : ResponseEntity<io.liquidsoftware.base.booking.adapter.`in`.web.api.v1.ScheduledAppointmentOutputDto> =
    WorkflowDispatcher.dispatch<AppointmentScheduledEvent>(
      ScheduleAppointmentCommand(draft.userId, draft.startTime, draft.duration)
    )
      .throwIfSpringError()
      .fold(
        { ResponseEntity.ok(io.liquidsoftware.base.booking.adapter.`in`.web.api.v1.ScheduleSuccessDto(it.appointmentDto)) },
        {
          when (it) {
            is ScheduleAppointmentError -> ResponseEntity.badRequest()
              .body(io.liquidsoftware.base.booking.adapter.`in`.web.api.v1.ScheduleErrorDto("Unexpected Error: ${it.error}"))
            else -> ResponseEntity.internalServerError()
              .body(io.liquidsoftware.base.booking.adapter.`in`.web.api.v1.ScheduleErrorDto("Unknown Error: ${it.message}"))
          }
        }
      )

  @PostMapping(value = [io.liquidsoftware.base.booking.adapter.`in`.web.V1BookingPaths.CANCEL_APPT])
  suspend fun cancel(@PathVariable appointmentId: String)
    : ResponseEntity<io.liquidsoftware.base.booking.adapter.`in`.web.api.v1.CancelAppointmentOutputDto> =
    WorkflowDispatcher.dispatch<AppointmentCancelledEvent>(CancelAppointmentCommand(appointmentId))
      .throwIfSpringError()
      .fold(
        { ResponseEntity.ok(io.liquidsoftware.base.booking.adapter.`in`.web.api.v1.CancelApptSuccessDto(it.appointmentDto)) },
        {
          when (it) {
            is CancelAppointmentError -> ResponseEntity.badRequest()
              .body(io.liquidsoftware.base.booking.adapter.`in`.web.api.v1.CancelApptErrorDto("Unexpected Error: ${it.error}"))
            else -> ResponseEntity.internalServerError()
              .body(io.liquidsoftware.base.booking.adapter.`in`.web.api.v1.CancelApptErrorDto("Unknown Error: ${it.message}"))
          }
        }
      )

}
