package ventures.dvx.common.security

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ventures.dvx.bridgekeeper.ROLE_SYSTEM_USER
import ventures.dvx.common.logging.LoggerDelegate

private fun getSystemAuthentication(): UsernamePasswordAuthenticationToken {
  val userDetails: UserDetails = User("SYSTEM", "", listOf(SimpleGrantedAuthority(ROLE_SYSTEM_USER.name)))
  return UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
}

suspend fun <T> runAsSuperUser(block: suspend () -> T): T =
  mono { block() }
    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(getSystemAuthentication()))
    .awaitSingle()

fun <T> Mono<T>.runAsSuperUser(): Mono<T> =
  this.contextWrite(ReactiveSecurityContextHolder.withAuthentication(getSystemAuthentication()))

fun <T> Flux<T>.runAsSuperUser(): Flux<T> =
  this.contextWrite(ReactiveSecurityContextHolder.withAuthentication(getSystemAuthentication()))

@Component
class ExecutionContext {
  val log by LoggerDelegate()

  suspend fun getCurrentUser(): UserDetails {
    val user = ReactiveSecurityContextHolder.getContext()
      .mapNotNull { it?.authentication }
      .doOnNext { log.debug("The current principal: ${it?.principal}") }
      .map { when (it) {
        is UserDetails -> it
        is UsernamePasswordAuthenticationToken -> (it.principal as UserDetails)
        else -> throw IllegalStateException("Unexpected Authentication type")
      }}
      .awaitSingle()
    return user;
  }

}
