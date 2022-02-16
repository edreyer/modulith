package io.liquidsoftware.base.booking.domain

import arrow.core.Some
import arrow.core.Validated.Companion.validNel
import arrow.core.valid
import arrow.core.validNel
import arrow.core.zip
import io.liquidsoftware.base.booking.AppointmentId
import io.liquidsoftware.base.user.UserId
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclRole
import io.liquidsoftware.common.types.ValidationErrorNel
import io.liquidsoftware.common.types.ensure
import org.valiktor.functions.isValid
import org.valiktor.validate
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal interface AppointmentFields {
  val id: AppointmentId
  val userId: UserId
  val startTime: LocalDateTime
  val duration: Duration
}

data class AppointmentData(
  override val id: AppointmentId,
  override val userId: UserId,
  override val startTime: LocalDateTime,
  override val duration: Duration
) : AppointmentFields

internal sealed class Appointment : AppointmentFields {
  fun acl() = Acl.of(id.value, userId.value, AclRole.MANAGER)

  companion object {
    fun startTimeValidator(startTime: LocalDateTime): ValidationErrorNel<Some<LocalDateTime>> = ensure {
      validate(Some(startTime)) {
        validate(Some<LocalDateTime>::value).isValid { it.toLocalDate().isAfter(LocalDate.now()) }
      }
    }
    fun durationValidator(duration: Long): ValidationErrorNel<Some<Long>> = ensure {
      validate(Some(duration)) {
        validate(Some<Long>::value).isValid { it > 0 }
        validate(Some<Long>::value).isValid { it < 60 * 1 }
      }
    }
  }
}

internal data class DraftAppointment(
  private val data: AppointmentData
) : Appointment(), AppointmentFields by data {
  companion object {
    fun of(userId: String, startTime: LocalDateTime, duration: Long):
      ValidationErrorNel<DraftAppointment> {
      return AppointmentId.create().validNel().zip (
        UserId.of(userId),
        startTimeValidator(startTime),
        durationValidator(duration)
      ) { aId, uId, st, d ->
        DraftAppointment(AppointmentData(aId,
          uId,
          st.value,
          Duration.ofMinutes(d.value)))
      }
    }
  }
}

internal data class ScheduledAppointment(
  private val data: AppointmentData
) : Appointment(), AppointmentFields by data {
  companion object {
    fun of(apptId: String, userId: String, startTime: LocalDateTime, duration: Long):
      ValidationErrorNel<ScheduledAppointment> {
      return AppointmentId.of(apptId).zip(
        UserId.of(userId),
        startTime.valid(),
        durationValidator(duration)
      ) { aId, uId, st, d ->
        ScheduledAppointment(AppointmentData(
          aId,
          uId,
          st,
          Duration.ofMinutes(d.value)))
      }
    }
    fun of(draft: DraftAppointment): ScheduledAppointment =
      ScheduledAppointment(
        AppointmentData(draft.id, draft.userId, draft.startTime, draft.duration)
      )
  }
}

internal data class InProgressAppointment(
  private val data: AppointmentData
) : Appointment(), AppointmentFields by data {
  companion object {
    fun of(apptId: String, userId: String, startTime: LocalDateTime, duration: Long):
      ValidationErrorNel<InProgressAppointment> {
      return AppointmentId.of(apptId).zip(
        UserId.of(userId),
        startTimeValidator(startTime),
        durationValidator(duration)
      ) { aId, uId, st, d ->
        InProgressAppointment(AppointmentData(
          aId,
          uId,
          st.value,
          Duration.ofMinutes(d.value)))
      }
    }
  }
}

internal data class CompleteAppointment(
  val completeDate: LocalDateTime,
  private val data: AppointmentData
) : Appointment(), AppointmentFields by data {
  companion object {
    fun of(apptId: String, userId: String, startTime: LocalDateTime, duration: Long, completeDate: LocalDateTime):
      ValidationErrorNel<CompleteAppointment> {
      return AppointmentId.of(apptId).zip(
        UserId.of(userId),
        startTimeValidator(startTime),
        durationValidator(duration),
        ensure {
          validate(Some(completeDate)) {
            validate(Some<LocalDateTime>::value).isValid { it.isAfter(startTime) }
          }
        } ,
      ) { aId, uId, st, d, cd ->
        CompleteAppointment(cd.value,
          AppointmentData(aId, uId, st.value, Duration.ofMinutes(d.value)))
      }
    }
  }
}

internal data class CancelledAppointment(
  val cancelDate: LocalDateTime,
  private val data: AppointmentData
) : Appointment(), AppointmentFields by data {
  companion object {
    fun of(apptId: String, userId: String, startTime: LocalDateTime, duration: Long, cancelDate: LocalDateTime):
      ValidationErrorNel<CancelledAppointment> {
      return AppointmentId.of(apptId).zip(
        UserId.of(userId),
        validNel(startTime),
        validNel(Duration.of(duration, ChronoUnit.MINUTES)),
        validNel(cancelDate),
      ) { aId, uId, st, d, cd ->
        CancelledAppointment(cd,
          AppointmentData(aId, uId, st, d))
      }
    }
    fun of(appt: DraftAppointment): CancelledAppointment =
      CancelledAppointment(
        LocalDateTime.now(),
        AppointmentData(appt.id, appt.userId, appt.startTime, appt.duration)
      )
    fun of(appt: ScheduledAppointment): CancelledAppointment =
      CancelledAppointment(
        LocalDateTime.now(),
        AppointmentData(appt.id, appt.userId, appt.startTime, appt.duration)
      )
  }
}
