package io.liquidsoftware.base.booking.application.workflows

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import io.liquidsoftware.base.booking.application.config.BookingBridgekeeperConfig
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCancelledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.AppointmentDtoData
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.CancelledAppointmentDto
import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentError
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.application.service.AppointmentStateService
import io.liquidsoftware.base.booking.domain.CancelledAppointment
import io.liquidsoftware.base.user.application.port.`in`.FindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.UserFoundEvent
import io.liquidsoftware.base.user.bridgekeeper.UserSecured
import io.liquidsoftware.bridgekeeper.BridgeKeeper
import io.liquidsoftware.common.events.EventPublisher
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.workflow.BaseSafeSecureWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import javax.annotation.PostConstruct

@Component
internal class CancelAppointmentWorkflow(
  private val eventPublisher: EventPublisher,
  private val apptStateService: io.liquidsoftware.base.booking.application.service.AppointmentStateService,
  private val findAppts: io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort,
  override val ec: ExecutionContext,
  @Qualifier(io.liquidsoftware.base.booking.application.config.BookingBridgekeeperConfig.BOOKING_BRIDGE_KEEPER) override val bk: BridgeKeeper
) : BaseSafeSecureWorkflow<CancelAppointmentCommand, AppointmentCancelledEvent>(),
  UserSecured<CancelAppointmentCommand> {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  override suspend fun userMatchingFn(request: CancelAppointmentCommand): Boolean {
    return WorkflowDispatcher.dispatch<UserFoundEvent>(FindUserByEmailQuery(ec.getCurrentUser().username))
        .fold({
            findAppts.findById(request.appointmentId)
            ?.let { appt -> appt.userId.value == it.userDto.id }
            ?: false
          }, {
            false
          }
        )
  }

  override suspend fun execute(request: CancelAppointmentCommand): AppointmentCancelledEvent {
    return findAppts.findById(request.appointmentId)
      ?.let { apptStateService.cancel(it) }
      ?.let { eventPublisher.publish(AppointmentCancelledEvent(it.toDto()))}
      ?: throw ScheduleAppointmentError.CancelAppointmentError("Failed to cancel appt ID='${request.appointmentId}'")
  }

  suspend fun io.liquidsoftware.base.booking.domain.CancelledAppointment.toDto(): CancelledAppointmentDto =
    CancelledAppointmentDto(AppointmentDtoData(
      this.id.value,
      this.userId.value,
      this.startTime,
      this.duration.toMinutes()),
      this.cancelDate
    )
}
