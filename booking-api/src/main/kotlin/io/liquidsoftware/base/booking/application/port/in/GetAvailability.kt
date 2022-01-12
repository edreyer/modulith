package io.liquidsoftware.base.booking.application.port.`in`

import io.liquidsoftware.common.workflow.Event
import io.liquidsoftware.common.workflow.Query
import java.time.LocalDate
import java.time.LocalTime

// Inputs
data class GetAvailabilityQuery(val date: LocalDate) : Query

// Outputs
data class AvailabilityRetrievedEvent(val times: List<LocalTime>) : Event()

// Errors
