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
  }
}

internal data class ReadyWorkOrder(
  private val data: WorkOrderData
) : WorkOrder(), WorkOrderFields by data {
  companion object {
    fun of(
      workOrderId: String = NamespaceIdGenerator.nextId(BookingNamespaces.WORK_WORDER_NS),
      service: String):
      ValidationErrorNel<ReadyWorkOrder> {
      return WorkOrderId.of(workOrderId).zip (
        NonEmptyString.of(service)
      ) { woId, s ->
        ReadyWorkOrder(WorkOrderData(woId, s))
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
        NonEmptyString.of(service)
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
      ) { woId, s, sd, cd, n ->
        CompleteWorkOrder(sd.value, cd.value, n, WorkOrderData(woId, s))
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

