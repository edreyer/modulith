package io.liquidsoftware.base.booking.adapter.out.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
internal interface WorkOrderRepository : JpaRepository<WorkOrderEntity, String>
