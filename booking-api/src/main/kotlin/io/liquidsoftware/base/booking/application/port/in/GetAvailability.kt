package io.liquidsoftware.base.booking.application.port.`in`

import io.liquidsoftware.common.usecase.AppEvent
import io.liquidsoftware.common.usecase.Query
import java.time.LocalDate
import java.time.LocalTime

// Inputs
data class GetAvailabilityQuery(val date: LocalDate) : Query

// Outputs
data class AvailabilityRetrievedEvent(val times: List<LocalTime>) : AppEvent()

// Errors
