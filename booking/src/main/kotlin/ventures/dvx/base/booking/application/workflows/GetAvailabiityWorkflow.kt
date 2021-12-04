package ventures.dvx.base.booking.application.workflows

import org.springframework.stereotype.Component
import ventures.dvx.base.booking.application.port.`in`.AvailabilityRetrievedEvent
import ventures.dvx.base.booking.application.port.`in`.GetAvailabilityQuery
import ventures.dvx.base.booking.application.port.out.FindAppointmentPort
import ventures.dvx.base.booking.application.service.AvailabilityService
import ventures.dvx.common.workflow.BaseSafeWorkflow
import ventures.dvx.common.workflow.WorkflowDispatcher
import javax.annotation.PostConstruct

@Component
internal class GetAvailabiityWorkflow(
  private val findApptsPost: FindAppointmentPort,
  private val availabilityService: AvailabilityService
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
