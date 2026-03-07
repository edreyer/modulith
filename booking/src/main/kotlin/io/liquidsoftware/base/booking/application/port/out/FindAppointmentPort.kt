package io.liquidsoftware.base.booking.application.port.out

import arrow.core.Either
import io.liquidsoftware.base.booking.domain.Appointment
import io.liquidsoftware.base.booking.domain.CompleteAppointment
import io.liquidsoftware.base.booking.domain.InProgressAppointment
import io.liquidsoftware.base.booking.domain.ScheduledAppointment
import io.liquidsoftware.common.workflow.WorkflowError
import org.springframework.data.domain.Pageable
import java.time.LocalDate

internal interface FindAppointmentPort {
  suspend fun findById(apptId: String): Either<WorkflowError, Appointment?>
  suspend fun findScheduledById(apptId: String): Either<WorkflowError, ScheduledAppointment?>
  suspend fun findStartedById(apptId: String): Either<WorkflowError, InProgressAppointment?>
  suspend fun findCompletedById(apptId: String): Either<WorkflowError, CompleteAppointment?>
  suspend fun findByUserId(userId: String, pageable: Pageable): Either<WorkflowError, List<Appointment>>
  suspend fun findAllForAvailability(date: LocalDate): Either<WorkflowError, List<Appointment>>
}
