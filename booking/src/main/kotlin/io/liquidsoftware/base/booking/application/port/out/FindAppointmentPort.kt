package io.liquidsoftware.base.booking.application.port.out

import io.liquidsoftware.base.booking.domain.Appointment
import java.time.LocalDate

internal interface FindAppointmentPort {
  suspend fun findById(apptId: String): io.liquidsoftware.base.booking.domain.Appointment?
  suspend fun findByUserId(userId: String): List<io.liquidsoftware.base.booking.domain.Appointment>
  suspend fun findAll(date: LocalDate): List<io.liquidsoftware.base.booking.domain.Appointment>
}
