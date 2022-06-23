package io.liquidsoftware.base.booking.adapter.out.persistence

import io.liquidsoftware.base.booking.BookingNamespaces
import io.liquidsoftware.base.booking.application.port.`in`.WorkOrderStatus
import io.liquidsoftware.common.persistence.BaseEntity
import io.liquidsoftware.common.persistence.NamespaceIdGenerator
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Table

@Entity
@Table(name = "work_orders")
internal class WorkOrderEntity(

  workOrderId: String = NamespaceIdGenerator.nextId(BookingNamespaces.WORK_WORDER_NS),

  var service: String,

  @Enumerated(EnumType.STRING)
  var status: WorkOrderStatus,

  var notes: String? = null,

  var startTime: LocalDateTime? = null,
  var completeTime: LocalDateTime? = null,
  var cancelTime: LocalDateTime? = null

  ) : BaseEntity(workOrderId, BookingNamespaces.WORK_WORDER_NS)
