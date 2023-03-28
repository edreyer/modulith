package io.liquidsoftware.base.booking.application.port.out

import io.liquidsoftware.base.booking.domain.Appointment
import io.liquidsoftware.base.booking.domain.CompleteAppointment
import io.liquidsoftware.base.booking.domain.InProgressAppointment
import io.liquidsoftware.base.booking.domain.ScheduledAppointment
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import java.time.LocalDate

internal interface FindAppointmentPort {
  suspend fun findById(apptId: String): Appointment?
  suspend fun findScheduledById(apptId: String): ScheduledAppointment?
  suspend fun findStartedById(apptId: String): InProgressAppointment?
  suspend fun findCompletedById(apptId: String): CompleteAppointment?
  suspend fun findByUserId(userId: String, pageable: Pageable): Flow<Appointment>
  suspend fun findAll(date: LocalDate): Flow<Appointment>
}
