package io.liquidsoftware.base.booking.application.workflows

import arrow.core.raise.Raise
import io.liquidsoftware.base.booking.application.port.`in`.AvailabilityRetrievedEvent
import io.liquidsoftware.base.booking.application.port.`in`.GetAvailabilityQuery
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.application.service.AvailabilityService
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowRegistry
import org.springframework.stereotype.Component

@Component
internal class GetAvailabilityWorkflow(
  private val findApptsPort: FindAppointmentPort,
  private val availabilityService: AvailabilityService
) : BaseSafeWorkflow<GetAvailabilityQuery, AvailabilityRetrievedEvent>() {

  override fun registerWithDispatcher() = WorkflowRegistry.registerQueryHandler(this)

  context(Raise<WorkflowError>)
  override suspend fun execute(request: GetAvailabilityQuery): AvailabilityRetrievedEvent {
    return findApptsPort.findAll(request.date)
      .let { availabilityService.getAvailability(it) }
      .let { AvailabilityRetrievedEvent(it) }
  }
}
