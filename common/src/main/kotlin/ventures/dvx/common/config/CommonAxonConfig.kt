package ventures.dvx.common.config

import org.axonframework.commandhandling.CommandBus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ventures.dvx.common.axon.security.AccessControlDispatchInterceptor

@Configuration
class CommonAxonConfig {

  @Bean
  fun accessControlDispatchInterceptor(commandBus: CommandBus)
  : AccessControlDispatchInterceptor {
    val interceptor = AccessControlDispatchInterceptor()
    commandBus.registerDispatchInterceptor(interceptor)
    return interceptor
  }

}
