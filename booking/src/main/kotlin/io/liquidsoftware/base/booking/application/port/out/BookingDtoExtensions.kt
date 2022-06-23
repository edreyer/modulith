package io.liquidsoftware.base.booking.application.port.out

import io.liquidsoftware.base.booking.application.port.`in`.AppointmentDtoOut
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStatus
import io.liquidsoftware.base.booking.application.port.`in`.WorkOrderDtoOut
import io.liquidsoftware.base.booking.application.port.`in`.WorkOrderStatus
import io.liquidsoftware.base.booking.domain.Appointment
import io.liquidsoftware.base.booking.domain.CancelledAppointment
import io.liquidsoftware.base.booking.domain.CancelledWorkOrder
import io.liquidsoftware.base.booking.domain.CompleteAppointment
import io.liquidsoftware.base.booking.domain.CompleteWorkOrder
import io.liquidsoftware.base.booking.domain.InProgressAppointment
import io.liquidsoftware.base.booking.domain.InProgressWorkOrder
import io.liquidsoftware.base.booking.domain.ReadyWorkOrder
import io.liquidsoftware.base.booking.domain.ScheduledAppointment
import io.liquidsoftware.base.booking.domain.WorkOrder

internal suspend fun Appointment.toDto(): AppointmentDtoOut =
  AppointmentDtoOut(
    id = this.id.value,
    userId = this.userId.value,
    duration = this.duration.toMinutes(),
    scheduledTime = this.scheduledTime,
    workOrderDto = this.workOrder.toDto(),
    status = when (this) {
      is ScheduledAppointment -> AppointmentStatus.SCHEDULED
      is InProgressAppointment -> AppointmentStatus.IN_PROGRESS
      is CompleteAppointment -> AppointmentStatus.COMPLETE
      is CancelledAppointment -> AppointmentStatus.CANCELLED
    },
    completeTime = when (this) {
      is CompleteAppointment -> this.completeTime
      else -> null
    },
    cancelTime = when (this) {
      is CancelledAppointment -> this.cancelTime
      else -> null
    }
  )

internal suspend fun WorkOrder.toDto() =
  WorkOrderDtoOut(
    id = id.value,
    service = service.value,
    status = when (this) {
      is ReadyWorkOrder -> WorkOrderStatus.READY
      is InProgressWorkOrder -> WorkOrderStatus.IN_PROGRESS
      is CompleteWorkOrder -> WorkOrderStatus.COMPLETE
      is CancelledWorkOrder -> WorkOrderStatus.CANCELLED
    },
    notes = when (this) {
      is CompleteWorkOrder -> this.notes
      is CancelledWorkOrder -> this.notes
      else -> null
    },
    startTime = when (this) {
      is InProgressWorkOrder -> this.startTime
      is CompleteWorkOrder -> this.startTime
      else -> null
    },
    completeTime = when (this) {
      is CompleteWorkOrder -> this.completeTime
      else -> null
    },
    cancelTime = when (this) {
      is CancelledWorkOrder -> this.cancelTime
      else -> null
    },
  )
