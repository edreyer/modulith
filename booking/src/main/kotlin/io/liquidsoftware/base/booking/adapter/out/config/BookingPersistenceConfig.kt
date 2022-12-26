package io.liquidsoftware.base.booking.adapter.out.config

import io.liquidsoftware.base.booking.adapter.out.persistence.AppointmentRepository
import io.liquidsoftware.base.booking.adapter.out.persistence.BookingPersistenceAdapter
import io.liquidsoftware.common.persistence.AuditorAwareImpl
import io.liquidsoftware.common.security.acl.AclChecker
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.ReactiveAuditorAware
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@Configuration
@EnableReactiveMongoRepositories("io.liquidsoftware.base.booking")
@EnableReactiveMongoAuditing
internal class BookingPersistenceConfig {

  @Bean
   fun appointmentPersistenceAdapter(
    appointmentRepository: AppointmentRepository,
    ac: AclChecker
  ): BookingPersistenceAdapter = BookingPersistenceAdapter(
    appointmentRepository,
    ac)

  @Bean
  fun auditorAware(): ReactiveAuditorAware<String> =
    AuditorAwareImpl()

}
