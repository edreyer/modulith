package io.liquidsoftware.base.booking.application.workflows

import arrow.core.raise.Raise
import io.liquidsoftware.base.booking.application.port.`in`.AvailabilityRetrievedEvent
import io.liquidsoftware.base.booking.application.port.`in`.DateInPastError
import io.liquidsoftware.base.booking.application.port.`in`.GetAvailabilityQuery
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.application.service.AvailabilityService
import io.liquidsoftware.common.ext.ensure
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowRegistry
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
internal class GetAvailabilityWorkflow(
  private val findApptsPort: FindAppointmentPort,
  private val availabilityService: AvailabilityService
) : BaseSafeWorkflow<GetAvailabilityQuery, AvailabilityRetrievedEvent>() {

  override fun registerWithDispatcher() = WorkflowRegistry.registerQueryHandler(this)

  context(_: Raise<WorkflowError>)
  override suspend fun execute(request: GetAvailabilityQuery): AvailabilityRetrievedEvent {
    ensure(request.date.isAfter(LocalDate.now())) { DateInPastError("${request.date} is in the past") }
    return findApptsPort.findAll(request.date)
      .let { availabilityService.getAvailability(it) }
      .let { AvailabilityRetrievedEvent(it) }
  }
}
