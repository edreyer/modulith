package io.liquidsoftware.base.booking.domain

import arrow.core.Some
import arrow.core.validNel
import arrow.core.zip
import io.liquidsoftware.base.booking.BookingNamespaces
import io.liquidsoftware.base.booking.WorkOrderId
import io.liquidsoftware.common.persistence.NamespaceIdGenerator
import io.liquidsoftware.common.types.NonEmptyString
import io.liquidsoftware.common.types.ValidationErrorNel
import io.liquidsoftware.common.types.ensure
import org.valiktor.functions.isValid
import org.valiktor.validate
import java.time.LocalDateTime

internal interface WorkOrderFields {
  val id: WorkOrderId
  val service: NonEmptyString
}

internal data class WorkOrderData(
  override val id: WorkOrderId,
  override val service: NonEmptyString
) : WorkOrderFields

internal sealed class
WorkOrder : WorkOrderFields {
  companion object {
    fun startDateValidator(startTime: LocalDateTime): ValidationErrorNel<Some<LocalDateTime>> = ensure {
      validate(Some(startTime)) {
        validate(Some<LocalDateTime>::value).isValid { it.isBefore(LocalDateTime.now()) }
      }
    }
    fun completeDateValidator(startTime:LocalDateTime, completeTime: LocalDateTime): ValidationErrorNel<Some<LocalDateTime>> = ensure {
      validate(Some(completeTime)) {
        validate(Some<LocalDateTime>::value).isValid { it.isBefore(LocalDateTime.now()) }
        validate(Some<LocalDateTime>::value).isValid { it.isAfter(startTime) }
      }
    }
    fun paymentDateValidator(completeTime:LocalDateTime, paymentTime: LocalDateTime): ValidationErrorNel<Some<LocalDateTime>> = ensure {
      validate(Some(paymentTime)) {
        validate(Some<LocalDateTime>::value).isValid { it.isBefore(LocalDateTime.now()) }
        validate(Some<LocalDateTime>::value).isValid { it.isAfter(completeTime) }
      }
    }

  }
}

internal data class ReadyWorkOrder(
  val notes: String?,
  private val data: WorkOrderData
) : WorkOrder(), WorkOrderFields by data {
  companion object {
    fun of(
      workOrderId: String = NamespaceIdGenerator.nextId(BookingNamespaces.WORK_WORDER_NS),
      service: String,
      notes: String? = null):
      ValidationErrorNel<ReadyWorkOrder> {
      return WorkOrderId.of(workOrderId).zip (
        NonEmptyString.of(service),
        notes.validNel()
      ) { woId, s, n ->
        ReadyWorkOrder(n, WorkOrderData(woId, s))
      }
    }
  }
}

internal data class InProgressWorkOrder(
  val startTime: LocalDateTime,
  private val data: WorkOrderData
) : WorkOrder(), WorkOrderFields by data {
  companion object {
    fun of(workOrderId: String, service: String, startTime: LocalDateTime):
      ValidationErrorNel<InProgressWorkOrder> {
      return WorkOrderId.of(workOrderId).zip (
        startDateValidator(startTime),
        NonEmptyString.of(service),
      ) { woId, sd, s ->
        InProgressWorkOrder(sd.value, WorkOrderData(woId, s))
      }
    }

    fun of(ready: ReadyWorkOrder): InProgressWorkOrder =
      InProgressWorkOrder(
        LocalDateTime.now(),
        WorkOrderData(ready.id, ready.service)
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
    fun of(workOrderId: String, service: String,
           startTime: LocalDateTime, completeTime: LocalDateTime, notes: String = ""):
      ValidationErrorNel<CompleteWorkOrder> {
      return WorkOrderId.of(workOrderId).zip (
        NonEmptyString.of(service),
        startDateValidator(startTime),
        completeDateValidator(startTime, completeTime),
        notes.validNel()
      ) { woId, s, st, ct, n ->
        CompleteWorkOrder(st.value, ct.value, n, WorkOrderData(woId, s))
      }
    }
    fun of(inProgress: InProgressWorkOrder, notes: String?): CompleteWorkOrder =
      CompleteWorkOrder(
        inProgress.startTime,
        LocalDateTime.now(),
        notes,
        WorkOrderData(inProgress.id, inProgress.service)
      )
  }
}

internal data class PaidWorkOrder(
  private val startTime: LocalDateTime,
  val completeTime: LocalDateTime,
  val paymentTime: LocalDateTime,
  val notes: String?,
  private val data: WorkOrderData
) : WorkOrder(), WorkOrderFields by data {
  companion object {
    fun of(workOrderId: String, service: String,
           startTime: LocalDateTime, completeTime: LocalDateTime,
           paymentTime: LocalDateTime, notes: String? = ""):
      ValidationErrorNel<PaidWorkOrder> {
      return WorkOrderId.of(workOrderId).zip (
        NonEmptyString.of(service),
        startDateValidator(startTime),
        completeDateValidator(startTime, completeTime),
        paymentDateValidator(completeTime, paymentTime),
        notes.validNel()
      ) { woId, s, st, et, pt, n ->
        PaidWorkOrder(st.value, et.value, pt.value, n, WorkOrderData(woId, s))
      }
    }
    fun of(complete: CompleteWorkOrder): PaidWorkOrder =
      PaidWorkOrder(
        complete.startTime,
        complete.completeTime,
        LocalDateTime.now(),
        complete.notes,
        WorkOrderData(complete.id, complete.service)
      )
  }
}

internal data class CancelledWorkOrder(
  val cancelTime: LocalDateTime,
  val notes: String,
  private val data: WorkOrderData
) : WorkOrder(), WorkOrderFields by data {
  companion object {
    fun of(workOrderId: String, service: String,
           cancelTime: LocalDateTime, notes: String = ""):
      ValidationErrorNel<CancelledWorkOrder> {
      return WorkOrderId.of(workOrderId).zip (
        NonEmptyString.of(service),
        notes.validNel()
      ) { woId, s, n ->
        CancelledWorkOrder(cancelTime, n, WorkOrderData(woId, s))
      }
    }
  }
}

