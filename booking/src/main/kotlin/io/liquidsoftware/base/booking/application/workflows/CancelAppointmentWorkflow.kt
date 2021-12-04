package io.liquidsoftware.base.booking.application.workflows

import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCancelledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.AppointmentDtoData
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.CancelledAppointmentDto
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentError
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.application.service.AppointmentStateService
import io.liquidsoftware.base.booking.domain.CancelledAppointment
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
internal class CancelAppointmentWorkflow(
  private val apptStateService: AppointmentStateService,
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) : BaseSafeWorkflow<CancelAppointmentCommand, AppointmentCancelledEvent>() {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  override suspend fun execute(request: CancelAppointmentCommand): AppointmentCancelledEvent {
    return findAppointmentPort.findById(request.appointmentId)
      ?.let { apptStateService.cancel(it) }
      ?.let { appointmentEventPort.handle(AppointmentCancelledEvent(it.toDto()))}
      ?: throw ScheduleAppointmentError.CancelAppointmentError("Failed to cancel appt ID='${request.appointmentId}'")
  }

  suspend fun CancelledAppointment.toDto(): CancelledAppointmentDto =
    CancelledAppointmentDto(AppointmentDtoData(
      this.id.value,
      this.userId.value,
      this.startTime,
      this.duration.toMinutes()),
      this.cancelDate
    )
}
