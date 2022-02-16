package io.liquidsoftware.base.booking.application.service

import io.liquidsoftware.base.booking.domain.Appointment
import org.springframework.stereotype.Service
import java.time.LocalTime

@Service
internal class AvailabilityService {

  suspend fun getAvailability(appts: List<Appointment>): List<LocalTime> {
    val existing = appts.map { it.startTime.toLocalTime().hour }
    return generateSequence(9) { it + 1 }.take(8)
      .filter { it !in existing }
      .map { LocalTime.of(it, 0) }
      .toList()
  }

  suspend fun isTimeAvailable(appts: List<Appointment>, time: LocalTime): Boolean =
    !appts.map { it.startTime.toLocalTime().hour }.contains(time.hour)

}
