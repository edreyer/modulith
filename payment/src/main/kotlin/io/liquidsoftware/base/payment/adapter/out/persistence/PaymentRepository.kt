package io.liquidsoftware.base.payment.adapter.out.persistence

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
internal interface PaymentRepository: MongoRepository<PaymentEntity, String> {

  fun findByUserId(userId: String) : PaymentEntity?

}
