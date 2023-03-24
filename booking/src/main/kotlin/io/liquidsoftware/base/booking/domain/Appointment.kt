package io.liquidsoftware.base.booking.domain

import arrow.core.Some
import arrow.core.continuations.EffectScope
import io.liquidsoftware.base.booking.AppointmentId
import io.liquidsoftware.base.booking.BookingNamespaces
import io.liquidsoftware.base.payment.PaymentId
import io.liquidsoftware.base.user.UserId
import io.liquidsoftware.common.persistence.NamespaceIdGenerator
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclRole
import io.liquidsoftware.common.types.ValidationErrors
import io.liquidsoftware.common.types.ensure
import org.valiktor.functions.isValid
import org.valiktor.validate
import java.time.Duration
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
    context(EffectScope<ValidationErrors>)
    suspend fun scheduledTimeValidator(startTime: LocalDateTime): LocalDateTime = ensure {
      validate(Some(startTime)) {
        validate(Some<LocalDateTime>::value).isValid { it.isAfter(LocalDateTime.now()) }
      }
    }.bind().value

    context(EffectScope<ValidationErrors>)
    suspend fun completeTimeValidator(startTime: LocalDateTime, completeTime: LocalDateTime): LocalDateTime = ensure {
      validate(Some(completeTime)) {
        validate(Some<LocalDateTime>::value).isValid { it.isAfter(startTime) }
      }
    }.bind().value

    context(EffectScope<ValidationErrors>)
    suspend fun durationValidator(duration: Long): Long = ensure {
      validate(Some(duration)) {
        validate(Some<Long>::value).isValid { it > 0 }
        validate(Some<Long>::value).isValid { it < 60 * 1 }
      }
    }.bind().value
  }
}

internal data class ScheduledAppointment(
  private val data: AppointmentData
) : Appointment(), AppointmentFields by data {
  companion object {
    context(EffectScope<ValidationErrors>)
    suspend fun of(apptId: String = NamespaceIdGenerator.nextId(BookingNamespaces.APPOINTMENT_NS),
           userId: String, scheduledTime: LocalDateTime, duration: Long, workOrder: ReadyWorkOrder): ScheduledAppointment =
      ScheduledAppointment(AppointmentData(
        AppointmentId.of(apptId),
        UserId.of(userId),
        workOrder,
        scheduledTime,
        Duration.ofMinutes(durationValidator(duration))
      ))
  }
}

internal data class InProgressAppointment(
  private val data: AppointmentData
) : Appointment(), AppointmentFields by data {
  companion object {
    context(EffectScope<ValidationErrors>)
    suspend fun of(apptId: String, userId: String, scheduledTime: LocalDateTime, duration: Long, workOrder: InProgressWorkOrder):
      InProgressAppointment = InProgressAppointment(AppointmentData(
        AppointmentId.of(apptId),
        UserId.of(userId),
        workOrder,
        scheduledTime,
        Duration.ofMinutes(durationValidator(duration))
      ))

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
    context(EffectScope<ValidationErrors>)
    suspend fun of(apptId: String, userId: String, startTime: LocalDateTime, duration: Long, workOrder: CompleteWorkOrder, completeTime: LocalDateTime):
      CompleteAppointment = CompleteAppointment(
        completeTimeValidator(startTime, completeTime),
        AppointmentData(
          AppointmentId.of(apptId),
          UserId.of(userId),
          workOrder,
          startTime,
          Duration.ofMinutes(durationValidator(duration))
        )
      )

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

  fun totalDue(): Long {
    // in a real appt, it would aggregate the value of the services on the work order,
    // substract any discounts/credits, calculate tax.
    // Here, we create a random value to keep it simple
    return 9000
  }
}

internal data class PaidAppointment(
  val paymentId: PaymentId,
  val completeTime: LocalDateTime,
  private val data: AppointmentData
) : Appointment(), AppointmentFields by data {
  companion object {
    context(EffectScope<ValidationErrors>)
    suspend fun of(apptId: String, paymentId: String, userId: String, startTime: LocalDateTime, duration: Long, workOrder: PaidWorkOrder, completeTime: LocalDateTime):
      PaidAppointment = PaidAppointment(
        PaymentId.of(paymentId),
        completeTimeValidator(startTime, completeTime),
        AppointmentData(
          AppointmentId.of(apptId),
          UserId.of(userId),
          workOrder,
          startTime,
          Duration.ofMinutes(durationValidator(duration))
        )
      )

    context(EffectScope<ValidationErrors>)
    suspend fun of (completeAppointment: CompleteAppointment, paymentId: String): PaidAppointment {
      return with(completeAppointment) {
        PaidAppointment(
          PaymentId.of(paymentId), completeTime, AppointmentData(
            id, userId , PaidWorkOrder.of(workOrder as CompleteWorkOrder), scheduledTime, duration)
        )
      }
    }

  }
}

internal data class CancelledAppointment(
  val cancelTime: LocalDateTime,
  private val data: AppointmentData
) : Appointment(), AppointmentFields by data {
  companion object {
    context(EffectScope<ValidationErrors>)
    suspend fun of(apptId: String, userId: String, startTime: LocalDateTime, duration: Long, workOrder: WorkOrder, cancelDate: LocalDateTime):
      CancelledAppointment {
      return CancelledAppointment(
        cancelDate,
        AppointmentData(
          AppointmentId.of(apptId),
          UserId.of(userId),
          CancelledWorkOrder.of(workOrder.service.value, LocalDateTime.now()),
          startTime,
          Duration.of(durationValidator(duration), ChronoUnit.MINUTES)))
    }
    fun of(appt: Appointment): CancelledAppointment =
      CancelledAppointment(
        LocalDateTime.now(),
        AppointmentData(appt.id, appt.userId, appt.workOrder, appt.scheduledTime, appt.duration)
      )
  }
}
