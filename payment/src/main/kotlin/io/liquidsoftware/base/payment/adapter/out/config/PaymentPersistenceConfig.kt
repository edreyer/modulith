package io.liquidsoftware.base.payment.adapter.out.config

import io.liquidsoftware.base.payment.adapter.out.persistence.PaymentMethodRepository
import io.liquidsoftware.base.payment.adapter.out.persistence.PaymentPersistenceAdapter
import io.liquidsoftware.base.payment.adapter.out.persistence.PaymentRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@Configuration
@EnableReactiveMongoRepositories("io.liquidsoftware.base.payment")
internal class PaymentPersistenceConfig {

  @Bean
   fun paymentPersistenceAdapter(
    paymentMethodRepository: PaymentMethodRepository,
    paymentRepository: PaymentRepository
  ): PaymentPersistenceAdapter = PaymentPersistenceAdapter(
    paymentMethodRepository,
    paymentRepository)

}
