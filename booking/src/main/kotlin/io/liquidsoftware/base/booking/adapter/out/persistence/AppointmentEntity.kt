package io.liquidsoftware.base.booking.adapter.out.persistence

import io.liquidsoftware.base.booking.BookingNamespaces
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentCancelledEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentEvent
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentScheduledEvent
import io.liquidsoftware.common.persistence.BaseEntity
import io.liquidsoftware.common.persistence.NamespaceIdGenerator
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclRole
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.hibernate.annotations.Where
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Table

internal enum class AppointmentStatus {
  DRAFT,
  SCHEDULED,
  IN_PROGRESS,
  COMPLETE,
  CANCELLED
}

@Entity
@Table(name = "appointments")
@Where(clause = "deleted_at is null")
@FilterDef(name = "deletedProductFilter")
@Filter(name = "deletedProductFilter", condition = "deleted_at is not null")
internal class AppointmentEntity(

  apptId: String = NamespaceIdGenerator.nextId(BookingNamespaces.APPOINTMENT_NS),

  var userId: String, // FK to User Aggregate

  var startTime: LocalDateTime,
  var duration: Long,

  @Enumerated(EnumType.STRING)
  var status: AppointmentStatus,

  var completeDate: LocalDateTime? = null,
  var cancelDate: LocalDateTime? = null

) : BaseEntity(apptId, BookingNamespaces.APPOINTMENT_NS) {

  fun acl() = Acl.of(id, userId, AclRole.MANAGER)

  fun handle(event: AppointmentEvent): AppointmentEntity {
    return when(event) {
      is AppointmentScheduledEvent -> handle(event)
      is AppointmentCancelledEvent -> handle(event)
    }
  }

  private fun handle(event: AppointmentScheduledEvent): AppointmentEntity {
    this.startTime = event.appointmentDto.startTime
    this.duration = event.appointmentDto.duration
    this.status = AppointmentStatus.SCHEDULED
    return this
  }

  private fun handle(event: AppointmentCancelledEvent): AppointmentEntity {
    this.status = AppointmentStatus.CANCELLED
    this.cancelDate = if (cancelDate == null) LocalDateTime.now() else this.cancelDate
    return this
  }

}
