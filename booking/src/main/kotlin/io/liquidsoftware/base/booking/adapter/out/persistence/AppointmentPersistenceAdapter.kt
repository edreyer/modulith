package io.liquidsoftware.base.booking.adapter.out.persistence

import arrow.core.Nel
import arrow.core.identity
import arrow.core.nonEmptyListOf
import io.liquidsoftware.base.booking.adapter.out.persistence.AppointmentStatus.CANCELLED
import io.liquidsoftware.base.booking.adapter.out.persistence.AppointmentStatus.COMPLETE
import io.liquidsoftware.base.booking.adapter.out.persistence.AppointmentStatus.DRAFT
import io.liquidsoftware.base.booking.adapter.out.persistence.AppointmentStatus.IN_PROGRESS
import io.liquidsoftware.base.booking.adapter.out.persistence.AppointmentStatus.SCHEDULED
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.CompleteAppointmentDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.DraftAppointmentDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.InProgressAppointmentDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDto.ScheduledAppointmentDto
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentEvent
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.types.ValidationError
import io.liquidsoftware.common.types.ValidationException
import io.liquidsoftware.common.types.toErrString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.context.event.EventListener
import java.time.LocalDate

internal class AppointmentPersistenceAdapter(
  private val apptRepository: io.liquidsoftware.base.booking.adapter.out.persistence.AppointmentRepository
) : io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort {

  private val logger by LoggerDelegate()

  override suspend fun findById(apptId: String): io.liquidsoftware.base.booking.domain.Appointment? =
    withContext(Dispatchers.IO) {
      apptRepository.findByAppointmentId(apptId)
        ?.toAppointment()
    }

  override suspend fun findByUserId(userId: String): List<io.liquidsoftware.base.booking.domain.Appointment> =
    withContext(Dispatchers.IO) {
      apptRepository.findByUserId(userId)
        .map { it.toAppointment() }
    }

  override suspend fun findAll(date: LocalDate): List<io.liquidsoftware.base.booking.domain.Appointment> =
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

  private fun io.liquidsoftware.base.booking.adapter.out.persistence.AppointmentEntity.toAppointment(): io.liquidsoftware.base.booking.domain.Appointment {
    val errorHandler = { errors: Nel<ValidationError> ->
      val err = errors.toErrString()
      logger.error(err)
      throw ValidationException(errors)
    }
    return when (this.status) {
      DRAFT -> errorHandler(nonEmptyListOf(ValidationError("Impossible: Cannot load a draft appointment from DB")))
      SCHEDULED -> io.liquidsoftware.base.booking.domain.ScheduledAppointment.of(
          this.id, this.userId, this.startTime, this.duration
        ).fold(errorHandler, ::identity)
      IN_PROGRESS -> io.liquidsoftware.base.booking.domain.InProgressAppointment.of(
          this.id, this.userId, this.startTime, this.duration
        ).fold(errorHandler, ::identity)
      COMPLETE -> io.liquidsoftware.base.booking.domain.CompleteAppointment.of(
        this.id, this.userId, this.startTime, this.duration, this.completeDate!!
      ).fold(errorHandler, ::identity)
      CANCELLED -> io.liquidsoftware.base.booking.domain.CancelledAppointment.of(
        this.id, this.userId, this.startTime, this.duration, this.cancelDate!!
      ).fold(errorHandler, ::identity)
    }
  }

  private fun AppointmentDto.toEntity(): io.liquidsoftware.base.booking.adapter.out.persistence.AppointmentEntity = when (this) {
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
