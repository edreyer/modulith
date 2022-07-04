package io.liquidsoftware.base.payment.adapter.out.config

import io.liquidsoftware.base.payment.adapter.out.persistence.PaymentMethodRepository
import io.liquidsoftware.base.payment.adapter.out.persistence.PaymentPersistenceAdapter
import io.liquidsoftware.base.payment.adapter.out.persistence.PaymentRepository
import io.liquidsoftware.common.security.acl.AclChecker
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories("io.liquidsoftware.base.payment")
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

}
