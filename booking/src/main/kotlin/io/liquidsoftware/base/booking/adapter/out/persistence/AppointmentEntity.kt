package io.liquidsoftware.base.booking.adapter.out.persistence

import io.liquidsoftware.base.booking.BookingNamespaces
import io.liquidsoftware.base.booking.application.port.`in`.*
import io.liquidsoftware.common.persistence.BaseEntity
import io.liquidsoftware.common.persistence.NamespaceIdGenerator
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclRole
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "appointments")
internal class AppointmentEntity(

  apptId: String = NamespaceIdGenerator.nextId(BookingNamespaces.APPOINTMENT_NS),

  var userId: String, // FK to User Aggregate

  var scheduledTime: LocalDateTime,
  var duration: Long,

  @Enumerated(EnumType.STRING)
  var status: AppointmentStatus,

  var completeTime: LocalDateTime? = null,
  var cancelTime: LocalDateTime? = null,

  @ManyToOne(cascade = [CascadeType.PERSIST])
  @JoinColumn(name = "work_order_id")
  var workOrder: WorkOrderEntity,

  var paymentId: String? = null

) : BaseEntity(apptId, BookingNamespaces.APPOINTMENT_NS) {

  fun acl() = Acl.of(id, userId, AclRole.MANAGER)

  fun handle(event: AppointmentEvent): AppointmentEntity {
    return when(event) {
      is AppointmentScheduledEvent -> handle(event)
      is AppointmentStartedEvent -> handle(event)
      is AppointmentCompletedEvent -> handle(event)
      is AppointmentPaidEvent -> handle(event)
      is AppointmentCancelledEvent -> handle(event)
    }
  }

  private fun handle(event: AppointmentScheduledEvent): AppointmentEntity {
    scheduledTime = event.appointmentDto.scheduledTime
    duration = event.appointmentDto.duration
    status = AppointmentStatus.SCHEDULED
    workOrder = WorkOrderEntity(
      service = event.appointmentDto.workOrderDto.service,
      status = WorkOrderStatus.READY
    )
    return this
  }

  private fun handle(event: AppointmentStartedEvent): AppointmentEntity {
    status = event.appointmentDto.status

    workOrder.startTime = event.appointmentDto.workOrderDto.startTime
    workOrder.status = event.appointmentDto.workOrderDto.status
    return this
  }

  private fun handle(event: AppointmentCompletedEvent): AppointmentEntity {
    status = AppointmentStatus.COMPLETE
    completeTime = event.appointmentDto.completeTime

    workOrder.status = WorkOrderStatus.COMPLETE
    workOrder.completeTime = event.appointmentDto.workOrderDto.completeTime
    workOrder.notes = event.appointmentDto.workOrderDto.notes
    return this
  }

  private fun handle(event: AppointmentPaidEvent): AppointmentEntity {
    status = event.appointmentDto.status
    cancelTime = if (cancelTime == null) LocalDateTime.now() else cancelTime
    paymentId = event.appointmentDto.paymentId

    workOrder.status = WorkOrderStatus.CANCELLED
    workOrder.cancelTime = if (workOrder.cancelTime == null) LocalDateTime.now() else workOrder.cancelTime
    workOrder.notes = event.appointmentDto.workOrderDto.notes
    return this
  }

  private fun handle(event: AppointmentCancelledEvent): AppointmentEntity {
    status = AppointmentStatus.CANCELLED
    cancelTime = if (cancelTime == null) LocalDateTime.now() else cancelTime

    workOrder.status = WorkOrderStatus.CANCELLED
    workOrder.cancelTime = if (workOrder.cancelTime == null) LocalDateTime.now() else workOrder.cancelTime
    workOrder.notes = event.appointmentDto.workOrderDto.notes
    return this
  }

}
