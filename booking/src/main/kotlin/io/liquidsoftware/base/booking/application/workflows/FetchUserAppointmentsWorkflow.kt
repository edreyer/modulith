package io.liquidsoftware.base.booking.application.workflows

import arrow.core.raise.Raise
import io.liquidsoftware.base.booking.application.mapper.toDto
import io.liquidsoftware.base.booking.application.port.`in`.FetchUserAppointmentsQuery
import io.liquidsoftware.base.booking.application.port.`in`.UserAppointmentsFetchedEvent
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.domain.CancelledAppointment
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowRegistry
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
internal class FetchUserAppointmentsWorkflow(
  private val findApptsPort: FindAppointmentPort,
) : BaseSafeWorkflow<FetchUserAppointmentsQuery, UserAppointmentsFetchedEvent>() {

  override fun registerWithDispatcher() = WorkflowRegistry.registerQueryHandler(this)

  context(_: Raise<WorkflowError>)
  override suspend fun execute(request: FetchUserAppointmentsQuery): UserAppointmentsFetchedEvent =
    findApptsPort.findByUserId(request.userId, PageRequest.of(request.page, request.size))
      .filter { it !is CancelledAppointment }
      .map { it.toDto() }
      .let { UserAppointmentsFetchedEvent(it) }

}
