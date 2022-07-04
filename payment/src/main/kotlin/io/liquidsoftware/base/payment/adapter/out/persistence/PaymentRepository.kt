package io.liquidsoftware.base.payment.adapter.out.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
internal interface PaymentRepository: JpaRepository<PaymentEntity, String> {

  fun findByUserId(userId: String) : PaymentEntity?

}
