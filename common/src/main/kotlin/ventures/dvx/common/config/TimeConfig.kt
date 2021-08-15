package ventures.dvx.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class TimeConfig {

  @Bean
  fun getClock(): Clock = Clock.systemDefaultZone()

}
