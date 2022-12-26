package io.liquidsoftware.base.booking.adapter.out.persistence

import io.liquidsoftware.base.booking.BookingNamespaces
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCancelledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCompletedEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentPaidEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentScheduledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStartedEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentStatus
import io.liquidsoftware.common.persistence.BaseMongoEntity
import io.liquidsoftware.common.persistence.NamespaceIdGenerator
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclRole
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("appointments")
internal class AppointmentEntity(

  @Indexed(unique = true)
  var appointmentId: String = NamespaceIdGenerator.nextId(BookingNamespaces.APPOINTMENT_NS),

  @Indexed
  var userId: String, // FK to User Aggregate

  var workOrder: WorkOrderEmbedded,

  var scheduledTime: LocalDateTime,
  var duration: Long,

  var status: AppointmentStatus,

  var completeTime: LocalDateTime? = null,
  var cancelTime: LocalDateTime? = null,

  var paymentId: String? = null

) : BaseMongoEntity(appointmentId, BookingNamespaces.APPOINTMENT_NS) {

  fun acl() = Acl.of(appointmentId, userId, AclRole.MANAGER)

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
    workOrder.handle(event)
    return this
  }

  private fun handle(event: AppointmentStartedEvent): AppointmentEntity {
    status = event.appointmentDto.status
    workOrder.handle(event)
    return this
  }

  private fun handle(event: AppointmentCompletedEvent): AppointmentEntity {
    status = AppointmentStatus.COMPLETE
    completeTime = event.appointmentDto.completeTime
    workOrder.handle(event)
    return this
  }

  private fun handle(event: AppointmentPaidEvent): AppointmentEntity {
    status = event.appointmentDto.status
    cancelTime = if (cancelTime == null) LocalDateTime.now() else cancelTime
    paymentId = event.appointmentDto.paymentId
    workOrder.handle(event)
    return this
  }

  private fun handle(event: AppointmentCancelledEvent): AppointmentEntity {
    status = AppointmentStatus.CANCELLED
    cancelTime = if (cancelTime == null) LocalDateTime.now() else cancelTime
    workOrder.handle(event)
    return this
  }

}
