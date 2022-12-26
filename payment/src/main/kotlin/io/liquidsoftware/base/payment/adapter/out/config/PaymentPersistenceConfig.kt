package io.liquidsoftware.base.payment.adapter.out.config

import io.liquidsoftware.base.payment.adapter.out.persistence.PaymentMethodRepository
import io.liquidsoftware.base.payment.adapter.out.persistence.PaymentPersistenceAdapter
import io.liquidsoftware.base.payment.adapter.out.persistence.PaymentRepository
import io.liquidsoftware.common.persistence.AuditorAwareImpl
import io.liquidsoftware.common.security.acl.AclChecker
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.ReactiveAuditorAware
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@Configuration
@EnableReactiveMongoRepositories("io.liquidsoftware.base.payment")
@EnableReactiveMongoAuditing
internal class PaymentPersistenceConfig {

  @Bean
   fun paymentPersistenceAdapter(
    paymentMethodRepository: PaymentMethodRepository,
    paymentRepository: PaymentRepository,
    ac: AclChecker
  ): PaymentPersistenceAdapter = PaymentPersistenceAdapter(
    paymentMethodRepository,
    paymentRepository,
    ac)

  @Bean
  fun auditorAware(): ReactiveAuditorAware<String> = AuditorAwareImpl()

}
