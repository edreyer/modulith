package io.liquidsoftware.base.booking.domain

import arrow.core.Some
import arrow.core.Validated.Companion.validNel
import arrow.core.valid
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
  val workOrder: WorkOrder
  val scheduledTime: LocalDateTime
  val duration: Duration
}

internal data class AppointmentData(
  override val id: AppointmentId,
  override val userId: UserId,
  override val workOrder: WorkOrder,
  override val scheduledTime: LocalDateTime,
  override val duration: Duration,
) : AppointmentFields

internal sealed class Appointment : AppointmentFields {
  fun acl() = Acl.of(id.value, userId.value, AclRole.MANAGER)

  companion object {
    fun ScheduledTimeValidator(startTime: LocalDateTime): ValidationErrorNel<Some<LocalDateTime>> = ensure {
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

internal data class ScheduledAppointment(
  private val data: AppointmentData
) : Appointment(), AppointmentFields by data {
  companion object {
    fun of(apptId: String, userId: String, scheduledTime: LocalDateTime, duration: Long, workOrder: ReadyWorkOrder):
      ValidationErrorNel<ScheduledAppointment> {
      return AppointmentId.of(apptId).zip(
        UserId.of(userId),
        scheduledTime.valid(),
        durationValidator(duration)
      ) { aId, uId, st, d ->
        ScheduledAppointment(AppointmentData(
          aId, uId, workOrder, st, Duration.ofMinutes(d.value)
        ))
      }
    }
  }
}

internal data class InProgressAppointment(
  private val data: AppointmentData
) : Appointment(), AppointmentFields by data {
  companion object {
    fun of(apptId: String, userId: String, scheduledTime: LocalDateTime, duration: Long, workOrder: InProgressWorkOrder):
      ValidationErrorNel<InProgressAppointment> {
      return AppointmentId.of(apptId).zip(
        UserId.of(userId),
        ScheduledTimeValidator(scheduledTime),
        durationValidator(duration)
      ) { aId, uId, st, d ->
        InProgressAppointment(AppointmentData(
          aId, uId, workOrder, st.value, Duration.ofMinutes(d.value)
        ))
      }
    }

    fun of (scheduledAppointment: ScheduledAppointment) =
      scheduledAppointment.let {
        InProgressAppointment(
          AppointmentData(it.id, it.userId, InProgressWorkOrder.of(it.workOrder as ReadyWorkOrder),
            it.scheduledTime, it.duration)
        )
      }
  }
}

internal data class CompleteAppointment(
  val completeTime: LocalDateTime,
  private val data: AppointmentData
) : Appointment(), AppointmentFields by data {
  companion object {
    fun of(apptId: String, userId: String, startTime: LocalDateTime, duration: Long, workOrder: CompleteWorkOrder, completeDate: LocalDateTime):
      ValidationErrorNel<CompleteAppointment> {
      return AppointmentId.of(apptId).zip(
        UserId.of(userId),
        ScheduledTimeValidator(startTime),
        durationValidator(duration),
        ensure {
          validate(Some(completeDate)) {
            validate(Some<LocalDateTime>::value).isValid { it.isAfter(startTime) }
          }
        } ,
      ) { aId, uId, st, d, cd ->
        CompleteAppointment(cd.value,
          AppointmentData(aId, uId, workOrder, st.value, Duration.ofMinutes(d.value)))
      }
    }

    fun of (startedAppointment: InProgressAppointment, notes: String?) =
      startedAppointment.let {
        CompleteAppointment(
          LocalDateTime.now(),
          AppointmentData(it.id, it.userId,
            CompleteWorkOrder.of(it.workOrder as InProgressWorkOrder, notes),
            it.scheduledTime, it.duration)
        )
      }

  }
}

internal data class CancelledAppointment(
  val cancelTime: LocalDateTime,
  private val data: AppointmentData
) : Appointment(), AppointmentFields by data {
  companion object {
    fun of(apptId: String, userId: String, startTime: LocalDateTime, duration: Long, workOrder: WorkOrder, cancelDate: LocalDateTime):
      ValidationErrorNel<CancelledAppointment> {
      return AppointmentId.of(apptId).zip(
        UserId.of(userId),
        validNel(startTime),
        validNel(Duration.of(duration, ChronoUnit.MINUTES)),
        validNel(cancelDate),
      ) { aId, uId, st, d, cd ->
        CancelledAppointment(cd,
          AppointmentData(aId, uId, workOrder, st, d))
      }
    }
    fun of(appt: Appointment): CancelledAppointment =
      CancelledAppointment(
        LocalDateTime.now(),
        AppointmentData(appt.id, appt.userId, appt.workOrder, appt.scheduledTime, appt.duration)
      )
  }
}
