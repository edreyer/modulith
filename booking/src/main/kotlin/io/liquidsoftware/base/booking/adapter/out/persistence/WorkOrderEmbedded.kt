package io.liquidsoftware.base.booking.adapter.out.persistence

import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCancelledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCompletedEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentPaidEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentScheduledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStartedEvent
import io.liquidsoftware.base.booking.application.port.`in`.WorkOrderStatus
import java.time.LocalDateTime

internal class WorkOrderEmbedded(

  var service: String,

  var status: WorkOrderStatus,

  var notes: String? = null,

  var startTime: LocalDateTime? = null,
  var completeTime: LocalDateTime? = null,
  var paymentTime: LocalDateTime? = null,
  var cancelTime: LocalDateTime? = null

  ) {

  fun handle(event: AppointmentEvent): WorkOrderEmbedded {
    return when(event) {
      is AppointmentScheduledEvent -> handle(event)
      is AppointmentStartedEvent -> handle(event)
      is AppointmentCompletedEvent -> handle(event)
      is AppointmentPaidEvent -> handle(event)
      is AppointmentCancelledEvent -> handle(event)
    }
  }

  private fun handle(event: AppointmentScheduledEvent): WorkOrderEmbedded {
    service = event.appointmentDto.workOrderDto.service
    status = WorkOrderStatus.READY
    return this
  }

  private fun handle(event: AppointmentStartedEvent): WorkOrderEmbedded {
    startTime = event.appointmentDto.workOrderDto.startTime
    status = event.appointmentDto.workOrderDto.status
    return this
  }

  private fun handle(event: AppointmentCompletedEvent): WorkOrderEmbedded {
    status = WorkOrderStatus.COMPLETE
    completeTime = event.appointmentDto.workOrderDto.completeTime
    notes = event.appointmentDto.workOrderDto.notes
    return this
  }

  private fun handle(event: AppointmentPaidEvent): WorkOrderEmbedded {
    status = WorkOrderStatus.CANCELLED
    cancelTime = if (cancelTime == null) LocalDateTime.now() else cancelTime
    notes = event.appointmentDto.workOrderDto.notes
    return this
  }

  private fun handle(event: AppointmentCancelledEvent): WorkOrderEmbedded {
    status = WorkOrderStatus.CANCELLED
    cancelTime = if (cancelTime == null) LocalDateTime.now() else cancelTime
    notes = event.appointmentDto.workOrderDto.notes
    return this
  }

}
