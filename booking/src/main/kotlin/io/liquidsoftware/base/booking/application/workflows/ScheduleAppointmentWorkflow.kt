package io.liquidsoftware.base.booking.application.workflows

import arrow.core.computations.ResultEffect.bind
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.AppointmentDtoData
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.ScheduledAppointmentDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentScheduledEvent
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentError.AppointmentValidationError
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentError.DateTimeUnavailableError
import io.liquidsoftware.base.user.application.port.`in`.FindUserByIdQuery
import io.liquidsoftware.base.user.application.port.`in`.UserFoundEvent
import io.liquidsoftware.base.user.bridgekeeper.UserSecured
import io.liquidsoftware.bridgekeeper.BridgeKeeper
import io.liquidsoftware.common.events.EventPublisher
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.types.toErrString
import io.liquidsoftware.common.workflow.BaseSafeSecureWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.LocalTime
import javax.annotation.PostConstruct

@Component
internal class ScheduleAppointmentWorkflow(
  private val eventPublisher: EventPublisher,
  private val apptStateService: io.liquidsoftware.base.booking.application.service.AppointmentStateService,
  private val availabilityService: io.liquidsoftware.base.booking.application.service.AvailabilityService,
  private val findAppts: io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort,
  override val ec: ExecutionContext,
  @Qualifier(io.liquidsoftware.base.booking.application.config.BookingBridgekeeperConfig.BOOKING_BRIDGE_KEEPER) override val bk: BridgeKeeper
) : BaseSafeSecureWorkflow<ScheduleAppointmentCommand, AppointmentScheduledEvent>(),
  UserSecured<ScheduleAppointmentCommand> {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  override suspend fun userMatchingFn(request: ScheduleAppointmentCommand): Boolean {
    return WorkflowDispatcher.dispatch<UserFoundEvent>(FindUserByIdQuery(request.userId))
        .fold(
          { ec.getCurrentUser().username == it.userDto.email },
          { false }
        )
  }

  override suspend fun execute(request: ScheduleAppointmentCommand): AppointmentScheduledEvent {
    // business invariant we must check
    val todaysAppts = findAppts.findAll(request.startTime.toLocalDate())
    checkTimeAvailable(todaysAppts, request.startTime.toLocalTime())

    return io.liquidsoftware.base.booking.domain.DraftAppointment.of(request.userId, request.startTime, request.duration)
      .map { apptStateService.schedule(it) }
      .fold({
        Result.failure(AppointmentValidationError(it.toErrString()))
      }, {
        Result.success(eventPublisher.publish(AppointmentScheduledEvent(it.toDto())))
      }).bind()
  }

  private suspend fun checkTimeAvailable(
    todaysAppts: List<io.liquidsoftware.base.booking.domain.Appointment>,
    startTime: LocalTime,
  ) {
    if (!availabilityService.isTimeAvailable(todaysAppts, startTime)) {
      throw DateTimeUnavailableError("'$startTime' is no longer available.")
    }
  }

  suspend fun io.liquidsoftware.base.booking.domain.ScheduledAppointment.toDto(): ScheduledAppointmentDto =
    ScheduledAppointmentDto(AppointmentDtoData(
      this.id.value,
      this.userId.value,
      this.startTime,
      this.duration.toMinutes())
    )
}
