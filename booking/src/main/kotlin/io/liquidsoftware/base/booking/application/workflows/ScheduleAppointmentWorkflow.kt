package io.liquidsoftware.base.booking.application.workflows

import arrow.core.Nel
import arrow.core.continuations.either
import io.liquidsoftware.base.booking.AppointmentId
import io.liquidsoftware.base.booking.WorkOrderId
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDtoOut
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentScheduledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStatus
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentValidationError
import io.liquidsoftware.base.booking.application.port.`in`.DateTimeUnavailableError
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentCommand
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.application.port.out.toDto
import io.liquidsoftware.base.booking.application.service.AvailabilityService
import io.liquidsoftware.base.booking.domain.Appointment
import io.liquidsoftware.base.booking.domain.ReadyWorkOrder
import io.liquidsoftware.base.booking.domain.ScheduledAppointment
import io.liquidsoftware.common.types.ValidationError
import io.liquidsoftware.common.types.toErrString
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.stereotype.Component
import java.time.LocalTime
import javax.annotation.PostConstruct

@Component
internal class ScheduleAppointmentWorkflow(
  private val availabilityService: AvailabilityService,
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) : BaseSafeWorkflow<ScheduleAppointmentCommand, AppointmentScheduledEvent>() {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  override suspend fun execute(request: ScheduleAppointmentCommand): AppointmentScheduledEvent {
    // business invariant we must check
    val todaysAppts = findAppointmentPort.findAll(request.scheduledTime.toLocalDate())
    checkTimeAvailable(todaysAppts, request.scheduledTime.toLocalTime())

    return either<Nel<ValidationError>, Appointment> {
      val apptId = AppointmentId.create();
      val workOrderId = WorkOrderId.create();
      val wo = ReadyWorkOrder.of(workOrderId.value, request.workOrder.service).bind()
      val appt = ScheduledAppointment
        .of(apptId.value, request.userId, request.scheduledTime, request.duration, wo)
        .bind()
      appt
    }
    .fold({
      Result.failure(AppointmentValidationError(it.toErrString()))
    }, {
      Result.success(appointmentEventPort.handle(AppointmentScheduledEvent(it.toDto())))
    }).getOrThrow()
  }

  private suspend fun checkTimeAvailable(
    todaysAppts: List<Appointment>,
    startTime: LocalTime,
  ) {
    if (!availabilityService.isTimeAvailable(todaysAppts, startTime)) {
      throw DateTimeUnavailableError("'$startTime' is no longer available.")
    }
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
