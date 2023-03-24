package io.liquidsoftware.base.booking.application.service

import io.liquidsoftware.base.booking.domain.Appointment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.LocalTime

@Service
internal class AvailabilityService {

  suspend fun getAvailability(appts: Flow<Appointment>): List<LocalTime> {
    val existing = appts.map { it.scheduledTime.toLocalTime().hour }.toList()
    return generateSequence(0) { it + 1 }.take(24)
      .filter { it !in existing }
      .map { LocalTime.of(it, 0) }
      .toList()
  }

  suspend fun isTimeAvailable(appts: Flow<Appointment>, time: LocalTime): Boolean =
    getAvailability(appts).map { it.hour }.contains(time.hour)

}
