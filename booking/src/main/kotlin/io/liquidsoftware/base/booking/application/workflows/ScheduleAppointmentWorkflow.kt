package io.liquidsoftware.base.booking.application.workflows

import arrow.core.computations.ResultEffect.bind
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.AppointmentDtoData
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.ScheduledAppointmentDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentScheduledEvent
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentError.AppointmentValidationError
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentError.DateTimeUnavailableError
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.application.service.AppointmentStateService
import io.liquidsoftware.base.booking.application.service.AvailabilityService
import io.liquidsoftware.base.booking.domain.Appointment
import io.liquidsoftware.base.booking.domain.DraftAppointment
import io.liquidsoftware.base.booking.domain.ScheduledAppointment
import io.liquidsoftware.common.types.toErrString
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.stereotype.Component
import java.time.LocalTime
import javax.annotation.PostConstruct

@Component
internal class ScheduleAppointmentWorkflow(
  private val apptStateService: AppointmentStateService,
  private val availabilityService: AvailabilityService,
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) : BaseSafeWorkflow<ScheduleAppointmentCommand, AppointmentScheduledEvent>() {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  override suspend fun execute(request: ScheduleAppointmentCommand): AppointmentScheduledEvent {
    // business invariant we must check
    val todaysAppts = findAppointmentPort.findAll(request.startTime.toLocalDate())
    checkTimeAvailable(todaysAppts, request.startTime.toLocalTime())

    return DraftAppointment.of(request.userId, request.startTime, request.duration)
      .map { apptStateService.schedule(it) }
      .fold({
        Result.failure(AppointmentValidationError(it.toErrString()))
      }, {
        Result.success(appointmentEventPort.handle(AppointmentScheduledEvent(it.toDto())))
      }).bind()
  }

  private suspend fun checkTimeAvailable(
    todaysAppts: List<Appointment>,
    startTime: LocalTime,
  ) {
    if (!availabilityService.isTimeAvailable(todaysAppts, startTime)) {
      throw DateTimeUnavailableError("'$startTime' is no longer available.")
    }
  }

  suspend fun ScheduledAppointment.toDto(): ScheduledAppointmentDto =
    ScheduledAppointmentDto(AppointmentDtoData(
      this.id.value,
      this.userId.value,
      this.startTime,
      this.duration.toMinutes())
    )
}
