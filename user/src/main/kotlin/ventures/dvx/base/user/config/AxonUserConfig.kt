package ventures.dvx.base.user.config

import org.axonframework.commandhandling.CommandBus
import org.axonframework.common.caching.Cache
import org.axonframework.common.caching.WeakReferenceCache
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ventures.dxv.base.user.error.RemoteErrorMapper

@Configuration
class AxonUserConfig {

// disable interceptor: https://github.com/AxonFramework/AxonFramework/issues/1850
//  @Autowired
//  fun configureLoggingInterceptorFor(
//    commandBus: CommandBus,
//    userCreationDispatchInterceptor: UserCreationDispatchInterceptor
//  ) {
//    commandBus.registerDispatchInterceptor(userCreationDispatchInterceptor)
//  }

  @Autowired
  fun configureErrorMappingFor(
    commandBus: CommandBus,
    commandGateway: ReactorCommandGateway,
    queryGateway: ReactorQueryGateway
  ) {
    commandGateway.registerResultHandlerInterceptor{ _, result ->
      result.onErrorMap { RemoteErrorMapper.mapRemoteException(it) }
    }
    queryGateway.registerResultHandlerInterceptor{ _, result ->
      result.onErrorMap { RemoteErrorMapper.mapRemoteException(it) }
    }
  }

  @Bean
  fun userCache(): Cache {
    // TODO replace with proper caching
    return WeakReferenceCache()
  }


}
