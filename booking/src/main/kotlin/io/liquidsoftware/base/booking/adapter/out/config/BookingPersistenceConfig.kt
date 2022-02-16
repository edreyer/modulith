package io.liquidsoftware.base.booking.adapter.out.config

import io.liquidsoftware.base.booking.adapter.out.persistence.AppointmentPersistenceAdapter
import io.liquidsoftware.base.booking.adapter.out.persistence.AppointmentRepository
import io.liquidsoftware.common.security.acl.AclChecker
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories("io.liquidsoftware.base.booking")
internal class BookingPersistenceConfig {

  @Bean
   fun appointmentPersistenceAdapter(
    appointmentRepository: AppointmentRepository,
    ac: AclChecker
  ): AppointmentPersistenceAdapter = AppointmentPersistenceAdapter(appointmentRepository, ac)

}
