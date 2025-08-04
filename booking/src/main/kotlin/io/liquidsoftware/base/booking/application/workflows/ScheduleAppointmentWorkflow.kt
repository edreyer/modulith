package io.liquidsoftware.base.booking.application.workflows

import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.liquidsoftware.base.booking.application.mapper.toDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDtoOut
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentScheduledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStatus
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentValidationError
import io.liquidsoftware.base.booking.application.port.`in`.DateTimeUnavailableError
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentCommand
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.application.service.AvailabilityService
import io.liquidsoftware.base.booking.domain.Appointment
import io.liquidsoftware.base.booking.domain.ReadyWorkOrder
import io.liquidsoftware.base.booking.domain.ScheduledAppointment
import io.liquidsoftware.common.ext.raise
import io.liquidsoftware.common.types.ValidationErrors
import io.liquidsoftware.common.types.toErrString
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowRegistry
import org.springframework.stereotype.Component

@Component
internal class ScheduleAppointmentWorkflow(
  private val availabilityService: AvailabilityService,
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) : BaseSafeWorkflow<ScheduleAppointmentCommand, AppointmentScheduledEvent>() {

  override fun registerWithDispatcher() = WorkflowRegistry.registerCommandHandler(this)

  context(_: Raise<WorkflowError>)
  override suspend fun execute(request: ScheduleAppointmentCommand): AppointmentScheduledEvent {
    // business invariant we must check
    val scheduledAppts = findAppointmentPort.findAll(request.scheduledTime.toLocalDate())
    val scheduledTime = request.scheduledTime.toLocalTime()

    either<WorkflowError, Unit> {
      ensure(availabilityService.isTimeAvailable(scheduledAppts, scheduledTime)) {
        DateTimeUnavailableError("'$scheduledTime' is no longer available.")
      }
    }

    return either<ValidationErrors, Appointment> {
      val wo = ReadyWorkOrder.of(
        service = request.workOrder.service,
        notes = request.workOrder.notes
      )
      val appt = ScheduledAppointment.of(
        userId = request.userId,
        scheduledTime = request.scheduledTime,
        duration = request.duration,
        workOrder = wo
      )
      appt
    }
    .fold(
      { raise(AppointmentValidationError(it.toErrString())) },
      { appointmentEventPort.handle(AppointmentScheduledEvent(it.toDto())) }
    )
  }

  suspend fun ScheduledAppointment.toDto(): AppointmentDtoOut =
    AppointmentDtoOut(
      id = this.id.value,
      userId = this.userId.value,
      duration = this.duration.toMinutes(),
      scheduledTime = this.scheduledTime,
      workOrderDto = this.workOrder.toDto(),
      status = AppointmentStatus.SCHEDULED
    )
}
