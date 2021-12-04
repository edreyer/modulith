package ventures.dvx.base.booking.application.port.`in`

import ventures.dvx.common.workflow.Event
import ventures.dvx.common.workflow.Query
import java.time.LocalDate
import java.time.LocalTime

// Inputs
data class GetAvailabilityQuery(val date: LocalDate) : Query

// Outputs
data class AvailabilityRetrievedEvent(val times: List<LocalTime>) : Event()

// Errors
