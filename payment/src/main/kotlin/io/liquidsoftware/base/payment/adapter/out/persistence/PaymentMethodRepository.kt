package io.liquidsoftware.base.payment.adapter.out.persistence

import io.liquidsoftware.base.payment.PaymentMethodId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
internal interface PaymentMethodRepository: JpaRepository<PaymentMethodEntity, String> {

  fun findByUserId(userId: String) : PaymentMethodEntity?

}
