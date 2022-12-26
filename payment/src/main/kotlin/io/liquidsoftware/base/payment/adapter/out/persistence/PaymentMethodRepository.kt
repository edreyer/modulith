package io.liquidsoftware.base.payment.adapter.out.persistence

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
internal interface PaymentMethodRepository: ReactiveMongoRepository<PaymentMethodEntity, String> {

  fun findByPaymentMethodIdAndUserId(id: String, userId: String) : Mono<PaymentMethodEntity>

}
