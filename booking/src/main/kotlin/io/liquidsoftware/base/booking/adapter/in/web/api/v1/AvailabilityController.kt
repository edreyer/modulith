package io.liquidsoftware.base.booking.adapter.`in`.web.api.v1

import io.liquidsoftware.base.booking.adapter.`in`.web.V1BookingPaths
import io.liquidsoftware.base.booking.application.port.`in`.AvailabilityRetrievedEvent
import io.liquidsoftware.base.booking.application.port.`in`.GetAvailabilityQuery
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.workflow.WorkflowDispatcher
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
val log by LoggerDelegate()

  @GetMapping(value = [V1BookingPaths.AVAILABILITY])
  fun availability(@PathVariable date: LocalDate)
  : ResponseEntity<AvailabilityDto> {
    return WorkflowDispatcher.dispatch<AvailabilityRetrievedEvent>(GetAvailabilityQuery(date))
      .fold(
        { ResponseEntity.badRequest().body(AvailabilityErrors("Availability error: ${it.message}")) },
        { ResponseEntity.ok(AvailabileTimesDto(it.times)) }
      )

  }
}
