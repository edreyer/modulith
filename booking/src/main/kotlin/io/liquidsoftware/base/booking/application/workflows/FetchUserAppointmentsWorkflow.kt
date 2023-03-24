package io.liquidsoftware.base.booking.application.workflows

import arrow.core.continuations.EffectScope
import io.liquidsoftware.base.booking.application.mapper.toDto
import io.liquidsoftware.base.booking.application.port.`in`.FetchUserAppointmentsQuery
import io.liquidsoftware.base.booking.application.port.`in`.UserAppointmentsFetchedEvent
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.domain.CancelledAppointment
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import io.liquidsoftware.common.workflow.WorkflowError
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
internal class FetchUserAppointmentsWorkflow(
  private val findApptsPort: FindAppointmentPort,
) : BaseSafeWorkflow<FetchUserAppointmentsQuery, UserAppointmentsFetchedEvent>() {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerQueryHandler(this)

  context(EffectScope<WorkflowError>)
  override suspend fun execute(request: FetchUserAppointmentsQuery): UserAppointmentsFetchedEvent =
    findApptsPort.findByUserId(request.userId)
      .filter { it !is CancelledAppointment }
      .map { it.toDto() }
      .let { UserAppointmentsFetchedEvent(it) }

}
