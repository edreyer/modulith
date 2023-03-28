package io.liquidsoftware.base.booking.application.workflows

import arrow.core.continuations.EffectScope
import io.liquidsoftware.base.booking.application.port.`in`.AvailabilityRetrievedEvent
import io.liquidsoftware.base.booking.application.port.`in`.GetAvailabilityQuery
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.application.service.AvailabilityService
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import io.liquidsoftware.common.workflow.WorkflowError
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.delay
import org.springframework.stereotype.Component

@Component
internal class GetAvailabilityWorkflow(
  private val findApptsPort: FindAppointmentPort,
  private val availabilityService: AvailabilityService
) : BaseSafeWorkflow<GetAvailabilityQuery, AvailabilityRetrievedEvent>() {

  @PostConstruct
  fun registerWithDispatcher() {
    WorkflowDispatcher.registerQueryHandler(this)
  }

  context(EffectScope<WorkflowError>)
  override suspend fun execute(request: GetAvailabilityQuery): AvailabilityRetrievedEvent {
    return findApptsPort.findAll(request.date)
      .also {
        // fake latency
        delay(1000)
      }
      .let { availabilityService.getAvailability(it) }
      .let { AvailabilityRetrievedEvent(it) }
  }
}
