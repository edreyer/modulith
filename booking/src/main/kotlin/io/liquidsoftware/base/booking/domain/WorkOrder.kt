package io.liquidsoftware.base.booking.domain

import arrow.core.Some
import arrow.core.continuations.EffectScope
import io.liquidsoftware.common.types.NonEmptyString
import io.liquidsoftware.common.types.ValidationErrors
import io.liquidsoftware.common.types.ensure
import org.valiktor.functions.isValid
import org.valiktor.validate
import java.time.LocalDateTime

internal interface WorkOrderFields {
  val service: NonEmptyString
}

internal data class WorkOrderData(
  override val service: NonEmptyString
) : WorkOrderFields

internal sealed class
WorkOrder : WorkOrderFields {
  companion object {
    context(EffectScope<ValidationErrors>)
    suspend fun startDateValidator(startTime: LocalDateTime): LocalDateTime = ensure {
      validate(Some(startTime)) {
        validate(Some<LocalDateTime>::value).isValid { it.isBefore(LocalDateTime.now()) }
      }
    }.bind().value

    context(EffectScope<ValidationErrors>)
    suspend fun completeDateValidator(startTime:LocalDateTime, completeTime: LocalDateTime): LocalDateTime = ensure {
      validate(Some(completeTime)) {
        validate(Some<LocalDateTime>::value).isValid { it.isBefore(LocalDateTime.now()) }
        validate(Some<LocalDateTime>::value).isValid { it.isAfter(startTime) }
      }
    }.bind().value

    context(EffectScope<ValidationErrors>)
    suspend fun paymentDateValidator(completeTime:LocalDateTime, paymentTime: LocalDateTime): LocalDateTime = ensure {
      validate(Some(paymentTime)) {
        validate(Some<LocalDateTime>::value).isValid { it.isBefore(LocalDateTime.now()) }
        validate(Some<LocalDateTime>::value).isValid { it.isAfter(completeTime) }
      }
    }.bind().value

  }
}

internal data class ReadyWorkOrder(
  val notes: String?,
  private val data: WorkOrderData
) : WorkOrder(), WorkOrderFields by data {
  companion object {
    context(EffectScope<ValidationErrors>)
    suspend fun of(service: String, notes: String? = null): ReadyWorkOrder {
      return ReadyWorkOrder(notes, WorkOrderData(NonEmptyString.of(service)))
    }
  }
}

internal data class InProgressWorkOrder(
  val startTime: LocalDateTime,
  private val data: WorkOrderData
) : WorkOrder(), WorkOrderFields by data {
  companion object {
    context(EffectScope<ValidationErrors>)
    suspend fun of(service: String, startTime: LocalDateTime): InProgressWorkOrder =
      InProgressWorkOrder(startDateValidator(startTime), WorkOrderData(NonEmptyString.of(service)))

    fun of(ready: ReadyWorkOrder): InProgressWorkOrder =
      InProgressWorkOrder(
        LocalDateTime.now(),
        WorkOrderData(ready.service)
      )
  }
}

internal data class CompleteWorkOrder(
  val startTime: LocalDateTime,
  val completeTime: LocalDateTime,
  val notes: String?,
  private val data: WorkOrderData
) : WorkOrder(), WorkOrderFields by data {
  companion object {
    context(EffectScope<ValidationErrors>)
    suspend fun of(service: String,
           startTime: LocalDateTime, completeTime: LocalDateTime, notes: String = ""): CompleteWorkOrder =
      CompleteWorkOrder(
        startDateValidator(startTime),
        completeDateValidator(startTime, completeTime),
        notes,
        WorkOrderData(NonEmptyString.of(service)))
    fun of(inProgress: InProgressWorkOrder, notes: String?): CompleteWorkOrder =
      CompleteWorkOrder(
        inProgress.startTime,
        LocalDateTime.now(),
        notes,
        WorkOrderData(inProgress.service)
      )
  }
}


internal data class PaidWorkOrder(
  val startTime: LocalDateTime,
  val completeTime: LocalDateTime,
  val paymentTime: LocalDateTime,
  val notes: String?,
  private val data: WorkOrderData
) : WorkOrder(), WorkOrderFields by data {
  companion object {
    context(EffectScope<ValidationErrors>)
    suspend fun of(service: String,
           startTime: LocalDateTime, completeTime: LocalDateTime,
           paymentTime: LocalDateTime, notes: String? = ""): PaidWorkOrder =
      PaidWorkOrder(
             startDateValidator(startTime),
             completeDateValidator(startTime, completeTime),
             paymentDateValidator(completeTime, paymentTime),
             notes,
             WorkOrderData(NonEmptyString.of(service))
      )

    fun of(complete: CompleteWorkOrder): PaidWorkOrder =
      PaidWorkOrder(
        complete.startTime,
        complete.completeTime,
        LocalDateTime.now(),
        complete.notes,
        WorkOrderData(complete.service)
      )
  }
}

internal data class CancelledWorkOrder(
  val cancelTime: LocalDateTime,
  val notes: String,
  private val data: WorkOrderData
) : WorkOrder(), WorkOrderFields by data {
  companion object {
    context(EffectScope<ValidationErrors>)
    suspend fun of(service: String, cancelTime: LocalDateTime, notes: String = ""): CancelledWorkOrder =
      CancelledWorkOrder(
        cancelTime, notes, WorkOrderData(NonEmptyString.of(service))
      )
  }
}

