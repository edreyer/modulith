package io.liquidsoftware.base.payment.adapter.out.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
internal interface PaymentMethodRepository: JpaRepository<PaymentMethodEntity, String> {

  fun findByIdAndUserId(id: String, userId: String) : PaymentMethodEntity?

}
