package io.liquidsoftware.base.booking.adapter.`in`.web.api.v1

import io.liquidsoftware.base.booking.adapter.`in`.web.V1BookingPaths
import io.liquidsoftware.base.booking.application.port.`in`.AvailabilityRetrievedEvent
import io.liquidsoftware.base.booking.application.port.`in`.GetAvailabilityQuery
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalTime

sealed class AvailabilityDto
data class AvailabileTimesDto(val times: List<LocalTime>) : AvailabilityDto()
data class AvailabilityErrors(val errors: String) : AvailabilityDto()

@RestController
class AvailabilityController {

  @GetMapping(value = [V1BookingPaths.AVAILABILITY])
  suspend fun availability(@PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") date: LocalDate)
  : ResponseEntity<AvailabilityDto> {
    val event: Result<AvailabilityRetrievedEvent> = WorkflowDispatcher.dispatch(GetAvailabilityQuery(date))
    return event
      .fold(
        { ResponseEntity.ok(AvailabileTimesDto(it.times)) },
        { ResponseEntity.badRequest().body(AvailabilityErrors("Availability error: ${it.message}")) }
      )

  }
}
