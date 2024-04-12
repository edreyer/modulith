package io.liquidsoftware.base.payment.adapter.out.persistence

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
internal interface PaymentMethodRepository: MongoRepository<PaymentMethodEntity, String> {

  fun findByPaymentMethodIdAndUserId(id: String, userId: String) : PaymentMethodEntity?

}
