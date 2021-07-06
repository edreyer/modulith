package ventures.dvx.common.config

import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ventures.dvx.bridgekeeper.RoleHandle
import ventures.dvx.common.axon.security.AccessControlCommandDispatchInterceptor
import ventures.dvx.common.axon.security.AccessControlQueryDispatchInterceptor

@Configuration
class CommonAxonConfig {

  @Bean
  fun accessControlCommandDispatchInterceptor(
    commandGateway: ReactorCommandGateway,
    roleHandleMap: Map<String, RoleHandle>
  )
  : AccessControlCommandDispatchInterceptor =
    AccessControlCommandDispatchInterceptor(roleHandleMap)
      .also { commandGateway.registerDispatchInterceptor(it) }

  @Bean
  fun accessControlQueryDispatchInterceptor(
    queryGateway: ReactorQueryGateway,
    roleHandleMap: Map<String, RoleHandle>
  )
    : AccessControlQueryDispatchInterceptor =
    AccessControlQueryDispatchInterceptor(roleHandleMap)
      .also { queryGateway.registerDispatchInterceptor(it) }

}
