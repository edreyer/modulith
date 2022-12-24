package io.liquidsoftware.base.booking.application.workflows

import arrow.core.Nel
import arrow.core.continuations.EffectScope
import arrow.core.continuations.effect
import io.liquidsoftware.base.booking.application.mapper.toDto
import io.liquidsoftware.base.booking.application.port.`in`.*
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.application.service.AvailabilityService
import io.liquidsoftware.base.booking.domain.Appointment
import io.liquidsoftware.base.booking.domain.ReadyWorkOrder
import io.liquidsoftware.base.booking.domain.ScheduledAppointment
import io.liquidsoftware.common.types.ValidationError
import io.liquidsoftware.common.types.toErrString
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import io.liquidsoftware.common.workflow.WorkflowError
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
internal class ScheduleAppointmentWorkflow(
  private val availabilityService: AvailabilityService,
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) : BaseSafeWorkflow<ScheduleAppointmentCommand, AppointmentScheduledEvent>() {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  context(EffectScope<WorkflowError>)
  override suspend fun execute(request: ScheduleAppointmentCommand): AppointmentScheduledEvent {
    // business invariant we must check
    val scheduledAppts = findAppointmentPort.findAll(request.scheduledTime.toLocalDate())
    val scheduledTime = request.scheduledTime.toLocalTime()

    ensure(availabilityService.isTimeAvailable(scheduledAppts, scheduledTime)) {
      DateTimeUnavailableError("'$scheduledTime' is no longer available.")
    }

    return effect<Nel<ValidationError>, Appointment> {
      val wo = ReadyWorkOrder.of(
        service = request.workOrder.service,
        notes = request.workOrder.notes
      )
      val appt = ScheduledAppointment.of(
        userId = request.userId,
        scheduledTime = request.scheduledTime,
        duration = request.duration,
        workOrder = wo.bind()
      )
      appt.bind()
    }
    .fold(
      { shift(AppointmentValidationError(it.toErrString())) },
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
