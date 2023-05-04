package io.liquidsoftware.base.booking.application.workflows

import arrow.core.raise.Raise
import arrow.core.raise.ensureNotNull
import io.liquidsoftware.base.booking.application.mapper.toDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCompletedEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentNotFoundError
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStatus
import io.liquidsoftware.base.booking.application.port.`in`.CompleteAppointmentCommand
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.domain.CompleteAppointment
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import io.liquidsoftware.common.workflow.WorkflowError
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
internal class CompleteAppointmentWorkflow(
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) : BaseSafeWorkflow<CompleteAppointmentCommand, AppointmentCompletedEvent>() {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  context(Raise<WorkflowError>)
  override suspend fun execute(request: CompleteAppointmentCommand): AppointmentCompletedEvent {
    // business invariant we must check
    return ensureNotNull(findAppointmentPort.findStartedById(request.appointmentId)) {
      AppointmentNotFoundError(
        "Appointment Not Found. id=${request.appointmentId}, status=${AppointmentStatus.IN_PROGRESS}"
      )
    }
      .let { CompleteAppointment.of(it, request.notes) }
      .let { appointmentEventPort.handle(AppointmentCompletedEvent(it.toDto())) }
  }

}
