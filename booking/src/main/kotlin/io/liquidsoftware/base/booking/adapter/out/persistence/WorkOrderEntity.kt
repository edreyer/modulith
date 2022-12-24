package io.liquidsoftware.base.booking.adapter.out.persistence

import io.liquidsoftware.base.booking.BookingNamespaces
import io.liquidsoftware.base.booking.application.port.`in`.WorkOrderStatus
import io.liquidsoftware.common.persistence.BaseEntity
import io.liquidsoftware.common.persistence.NamespaceIdGenerator
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.LocalDateTime

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
  var paymentTime: LocalDateTime? = null,
  var cancelTime: LocalDateTime? = null

  ) : BaseEntity(workOrderId, BookingNamespaces.WORK_WORDER_NS)
