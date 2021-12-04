package ventures.dvx.base.booking.adapter.out.persistence

import arrow.core.Nel
import arrow.core.identity
import arrow.core.nonEmptyListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.context.event.EventListener
import ventures.dvx.base.booking.adapter.out.persistence.AppointmentStatus.CANCELLED
import ventures.dvx.base.booking.adapter.out.persistence.AppointmentStatus.COMPLETE
import ventures.dvx.base.booking.adapter.out.persistence.AppointmentStatus.DRAFT
import ventures.dvx.base.booking.adapter.out.persistence.AppointmentStatus.IN_PROGRESS
import ventures.dvx.base.booking.adapter.out.persistence.AppointmentStatus.SCHEDULED
import ventures.dvx.base.booking.application.port.`in`.AppointmentDto
import ventures.dvx.base.booking.application.port.`in`.AppointmentDto.CompleteAppointmentDto
import ventures.dvx.base.booking.application.port.`in`.AppointmentDto.DraftAppointmentDto
import ventures.dvx.base.booking.application.port.`in`.AppointmentDto.InProgressAppointmentDto
import ventures.dvx.base.booking.application.port.`in`.AppointmentDto.ScheduledAppointmentDto
import ventures.dvx.base.booking.application.port.`in`.AppointmentEvent
import ventures.dvx.base.booking.application.port.out.FindAppointmentPort
import ventures.dvx.base.booking.domain.Appointment
import ventures.dvx.base.booking.domain.CancelledAppointment
import ventures.dvx.base.booking.domain.CompleteAppointment
import ventures.dvx.base.booking.domain.InProgressAppointment
import ventures.dvx.base.booking.domain.ScheduledAppointment
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dvx.common.types.ValidationError
import ventures.dvx.common.types.ValidationException
import ventures.dvx.common.types.toErrString
import java.time.LocalDate

internal class AppointmentPersistenceAdapter(
  private val apptRepository: AppointmentRepository
) : FindAppointmentPort {

  private val logger by LoggerDelegate()

  override suspend fun findById(apptId: String): Appointment? =
    withContext(Dispatchers.IO) {
      apptRepository.findByAppointmentId(apptId)
        ?.toAppointment()
    }

  override suspend fun findByUserId(userId: String): List<Appointment> =
    withContext(Dispatchers.IO) {
      apptRepository.findByUserId(userId)
        .map { it.toAppointment() }
    }

  override suspend fun findAll(date: LocalDate): List<Appointment> =
    withContext(Dispatchers.IO) {
      apptRepository.findByStartTimeBetween(date.atStartOfDay(), date.atStartOfDay().plusDays(1))
        .map { it.toAppointment() }
    }

  @EventListener(AppointmentEvent::class)
  fun handle(event: AppointmentEvent) =
    when (val apptDto = event.appointmentDto) {
      is ScheduledAppointmentDto -> {
        apptRepository.findByAppointmentId(event.appointmentDto.id)
          ?.handle(event)
          ?.let { apptRepository.saveAndFlush(it) }
          ?: apptRepository.saveAndFlush(apptDto.toEntity()) // New Entity

      }
      else -> apptRepository.findByAppointmentId(event.appointmentDto.id)
        ?.handle(event)
        ?.let { apptRepository.saveAndFlush(it) }
    }

  private fun AppointmentEntity.toAppointment(): Appointment {
    val errorHandler = { errors: Nel<ValidationError> ->
      val err = errors.toErrString()
      logger.error(err)
      throw ValidationException(errors)
    }
    return when (this.status) {
      DRAFT -> errorHandler(nonEmptyListOf(ValidationError("Impossible: Cannot load a draft appointment from DB")))
      SCHEDULED -> ScheduledAppointment.of(
          this.id, this.userId, this.startTime, this.duration
        ).fold(errorHandler, ::identity)
      IN_PROGRESS -> InProgressAppointment.of(
          this.id, this.userId, this.startTime, this.duration
        ).fold(errorHandler, ::identity)
      COMPLETE -> CompleteAppointment.of(
        this.id, this.userId, this.startTime, this.duration, this.completeDate!!
      ).fold(errorHandler, ::identity)
      CANCELLED -> CancelledAppointment.of(
        this.id, this.userId, this.startTime, this.duration, this.cancelDate!!
      ).fold(errorHandler, ::identity)
    }
  }

  private fun AppointmentDto.toEntity(): AppointmentEntity = when (this) {
    is DraftAppointmentDto ->
      AppointmentEntity(
        apptId = this.id,
        userId = this.userId,
        startTime = this.startTime,
        duration = this.duration,
        status = SCHEDULED)
    is ScheduledAppointmentDto -> apptRepository.findById(this.id)
      .orElseGet { null }
      ?.let { dbAppt ->
        dbAppt.userId = this.userId
        dbAppt.startTime = this.startTime
        dbAppt.duration = this.duration
        dbAppt
      } ?: AppointmentEntity(
        apptId = this.id,
        userId = this.userId,
        startTime = this.startTime,
        duration = this.duration,
        status = SCHEDULED)
    is InProgressAppointmentDto -> apptRepository.getById(this.id)
      .let { dbAppt ->
        dbAppt.userId = this.userId
        dbAppt.startTime = this.startTime
        dbAppt.duration = this.duration
        dbAppt
      }
    is CompleteAppointmentDto -> apptRepository.getById(this.id)
      .let { dbAppt ->
        dbAppt.userId = this.userId
        dbAppt.startTime = this.startTime
        dbAppt.duration = this.duration
        dbAppt.completeDate = this.completeDate
        dbAppt
      }
    is AppointmentDto.CancelledAppointmentDto -> apptRepository.getById(this.id)
      .let { dbAppt ->
        dbAppt.userId = this.userId
        dbAppt.startTime = this.startTime
        dbAppt.duration = this.duration
        dbAppt.cancelDate = this.cancelDate
        dbAppt
      }
  }
}
