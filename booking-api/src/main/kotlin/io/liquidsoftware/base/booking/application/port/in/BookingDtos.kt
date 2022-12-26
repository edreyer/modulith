package io.liquidsoftware.base.booking.application.port.`in`

import java.time.LocalDateTime

enum class AppointmentStatus {
  SCHEDULED,
  IN_PROGRESS,
  COMPLETE,
  CANCELLED,
  PAID
}

data class AppointmentDtoIn (
  val id: String? = null,
  val duration: Long,
  val scheduledTime: LocalDateTime,
  val workOrder: WorkOrderDtoIn
)

data class AppointmentIdDtoIn (
  val id: String,
)

data class AppointmentCompletedDtoIn (
  val id: String,
  val notes: String? = null
)

data class AppointmentPaymentDto (
  val id: String,
  val paymentMethodId: String
)

data class AppointmentDtoOut (
  val id: String,
  val userId: String,
  val duration: Long,
  val scheduledTime: LocalDateTime,
  val workOrderDto: WorkOrderDtoOut,
  val status: AppointmentStatus,

  val completeTime: LocalDateTime? = null,
  val paymentId: String? = null,
  val cancelTime: LocalDateTime? = null,
)

enum class WorkOrderStatus {
  READY,
  IN_PROGRESS,
  COMPLETE,
  PAID,
  CANCELLED
}
data class WorkOrderDtoIn(
  val service: String,
  val notes: String? = null
)

data class WorkOrderDtoOut(
  val service: String,
  val status: WorkOrderStatus,

  val notes: String?,
  val startTime: LocalDateTime?,
  val completeTime: LocalDateTime?,
  val paymentTime: LocalDateTime?,
  val cancelTime: LocalDateTime?,
)
