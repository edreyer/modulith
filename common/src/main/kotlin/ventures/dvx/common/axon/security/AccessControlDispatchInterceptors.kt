package ventures.dvx.common.axon.security

import org.axonframework.commandhandling.CommandMessage
import org.axonframework.extensions.reactor.messaging.ReactorMessageDispatchInterceptor
import org.axonframework.messaging.MetaData
import org.axonframework.queryhandling.QueryMessage
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import reactor.core.publisher.Mono
import ventures.dvx.bridgekeeper.ANONYMOUS
import ventures.dvx.bridgekeeper.RoleHandle
import ventures.dvx.bridgekeeper.UserParty
import ventures.dvx.common.logging.LoggerDelegate

private fun UserDetails.toParty(roleHandleMap: Map<String, RoleHandle> ) =
  UserParty(
    id = username,
    roles = authorities.map {
        ga -> roleHandleMap[ga.authority]!!
    }.toSet()
  )

class AccessControlCommandDispatchInterceptor(
  private val roleHandleMap: Map<String, RoleHandle>
) : ReactorMessageDispatchInterceptor<CommandMessage<*>> {

  val log by LoggerDelegate()

  // Adds principal to the message metadata for Access checking later
  override fun intercept(message: Mono<CommandMessage<*>>): Mono<CommandMessage<*>> =
    message
      .flatMap { msg ->
        ReactiveSecurityContextHolder.getContext()
          .map { it?.authentication }
          .filter { it != null }
          .doOnNext { log.info("The principal: ${it?.principal}") }
          .map { when (it) {
            is UserDetails -> it.toParty(roleHandleMap)
            is UsernamePasswordAuthenticationToken -> (it.principal as UserDetails).toParty(roleHandleMap)
            else -> throw IllegalStateException("Unexpected Authentication type")
          }}
          .map {
            msg.withMetaData(MetaData.with("party", it))
          }
      }
      .switchIfEmpty(message.map {
        it.withMetaData(MetaData.with("party", ANONYMOUS))
      })
      .doOnNext { log.info("cmd: $it") }
}

class AccessControlQueryDispatchInterceptor(
  private val roleHandleMap: Map<String, RoleHandle>
) : ReactorMessageDispatchInterceptor<QueryMessage<*, *>> {

  val log by LoggerDelegate()

  // Adds principal to the message metadata for Access checking later
  override fun intercept(message: Mono<QueryMessage<*,*>>): Mono<QueryMessage<*,*>> =
    ReactiveSecurityContextHolder.getContext()
      .map { it?.authentication }
      .filter { it != null}
      .doOnNext { log.info("The principal: ${it?.principal}") }
      .map { when (it) {
        is UserDetails -> it.toParty(roleHandleMap)
        is UsernamePasswordAuthenticationToken -> (it.principal as UserDetails).toParty(roleHandleMap)
        else -> throw IllegalStateException("Unexpected Authentication type")
      }}
      .flatMap {
        message.map {
          msg -> msg.withMetaData(MetaData.with("party", it))
        }
      }
      .switchIfEmpty(message.map {
        it.withMetaData(MetaData.with("party", ANONYMOUS))
      })
      .doOnNext { log.info("cmd: $it") }
}

