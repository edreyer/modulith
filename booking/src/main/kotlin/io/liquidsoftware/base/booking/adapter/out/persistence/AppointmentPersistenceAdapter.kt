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
import io.liquidsoftware.base.booking.application.port.out.AppointmentEventPort
import io.liquidsoftware.base.booking.application.port.out.FindAppointmentPort
import io.liquidsoftware.base.booking.domain.Appointment
import io.liquidsoftware.base.booking.domain.CancelledAppointment
import io.liquidsoftware.base.booking.domain.CompleteAppointment
import io.liquidsoftware.base.booking.domain.InProgressAppointment
import io.liquidsoftware.base.booking.domain.ScheduledAppointment
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclChecker
import io.liquidsoftware.common.security.acl.AclRole
import io.liquidsoftware.common.security.acl.Permission
import io.liquidsoftware.common.types.ValidationError
import io.liquidsoftware.common.types.ValidationException
import io.liquidsoftware.common.types.toErrString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

internal class AppointmentPersistenceAdapter(
  private val apptRepository: AppointmentRepository,
  private val ac: AclChecker
) : FindAppointmentPort, AppointmentEventPort {

  private val logger by LoggerDelegate()

  override suspend fun findById(apptId: String): Appointment? =
    withContext(Dispatchers.IO) {
      apptRepository.findByAppointmentId(apptId)
        ?.toAppointment()
        ?.also { ac.checkPermission(it.acl(), Permission.READ)}
    }

  override suspend fun findByUserId(userId: String): List<Appointment> =
    withContext(Dispatchers.IO) {
      apptRepository.findByUserId(userId)
        .map {
          it.toAppointment()
            .also { appt -> ac.checkPermission(appt.acl(), Permission.READ)}
        }
    }

  override suspend fun findAll(date: LocalDate): List<Appointment> =
    withContext(Dispatchers.IO) {
      apptRepository.findByStartTimeBetween(date.atStartOfDay(), date.atStartOfDay().plusDays(1))
        .map {
          it.toAppointment()
            .also { appt -> ac.checkPermission(appt.acl(), Permission.READ)}
        }
    }

  override suspend fun <T: AppointmentEvent> handle(event: T): T = withContext(Dispatchers.IO) {
    when (val apptDto = event.appointmentDto) {
      is ScheduledAppointmentDto -> {
        apptRepository.findByAppointmentId(event.appointmentDto.id)
          ?.also { ac.checkPermission(it.acl(), Permission.WRITE) }
          ?.handle(event)
          ?.let { apptRepository.saveAndFlush(it) }
          ?: apptRepository.saveAndFlush(apptDto.toEntity()) // New Entity
        event
      }
      else -> {
        apptRepository.findByAppointmentId(event.appointmentDto.id)
          ?.also { ac.checkPermission(Acl.of(it.id, it.userId, AclRole.WRITER), Permission.WRITE) }
          ?.handle(event)
          ?.let { apptRepository.saveAndFlush(it) }
        event
      }
    }
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
