package io.liquidsoftware.base.booking.application.workflows

import io.liquidsoftware.base.booking.application.port.`in`.AvailabilityRetrievedEvent
import io.liquidsoftware.base.booking.application.port.`in`.GetAvailabilityQuery
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
internal class GetAvailabiityWorkflow(
  private val findApptsPost: io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort,
  private val availabilityService: io.liquidsoftware.base.booking.application.service.AvailabilityService
) : BaseSafeWorkflow<GetAvailabilityQuery, AvailabilityRetrievedEvent>() {

  @PostConstruct
  fun registerWithDispatcher() {
    WorkflowDispatcher.registerQueryHandler(this)
  }

  override suspend fun execute(request: GetAvailabilityQuery): AvailabilityRetrievedEvent {
    val appts = findApptsPost.findAll(request.date)
    val available = availabilityService.getAvailability(appts)
    return AvailabilityRetrievedEvent(available)
  }
}
