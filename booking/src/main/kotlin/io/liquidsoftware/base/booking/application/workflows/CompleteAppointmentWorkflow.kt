package io.liquidsoftware.base.booking.application.workflows

import arrow.core.raise.Raise
import io.liquidsoftware.base.booking.application.mapper.toDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCompletedEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentNotFoundError
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStatus
import io.liquidsoftware.base.booking.application.port.`in`.CompleteAppointmentCommand
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.domain.CompleteAppointment
import io.liquidsoftware.common.ext.ensureNotNull
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowRegistry
import org.springframework.stereotype.Component

@Component
internal class CompleteAppointmentWorkflow(
  private val findAppointmentPort: FindAppointmentPort,
  private val appointmentEventPort: AppointmentEventPort,
) : BaseSafeWorkflow<CompleteAppointmentCommand, AppointmentCompletedEvent>() {

  override fun registerWithDispatcher() = WorkflowRegistry.registerCommandHandler(this)

  context(_: Raise<WorkflowError>)
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
