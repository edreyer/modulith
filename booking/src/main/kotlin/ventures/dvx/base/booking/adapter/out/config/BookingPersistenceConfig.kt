package ventures.dvx.base.booking.adapter.out.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement
import ventures.dvx.base.booking.adapter.out.persistence.AppointmentPersistenceAdapter
import ventures.dvx.base.booking.adapter.out.persistence.AppointmentRepository

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories("ventures.dvx.base.booking")
internal class BookingPersistenceConfig {

  @Bean
   fun appointmentPersistenceAdapter(appointmentRepository: AppointmentRepository)
   : AppointmentPersistenceAdapter =
     AppointmentPersistenceAdapter(appointmentRepository)

}
