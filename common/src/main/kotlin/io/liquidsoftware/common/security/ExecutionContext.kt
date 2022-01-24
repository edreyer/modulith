package io.liquidsoftware.common.security

import io.liquidsoftware.bridgekeeper.ROLE_SYSTEM_USER
import io.liquidsoftware.common.logging.LoggerDelegate
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

private fun getSystemAuthentication(): UsernamePasswordAuthenticationToken {
  val userDetails = UserDetailsWithId("SYSTEM",
    User("SYSTEM", "", listOf(SimpleGrantedAuthority(ROLE_SYSTEM_USER.name))))
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

  companion object {
    const val ANONYMOUS_USER_ID = "u_anonymous"
  }

  suspend fun getCurrentUser(): UserDetailsWithId {
    val user = ReactiveSecurityContextHolder.getContext()
      .mapNotNull { it?.authentication }
      .doOnNext { log.debug("The current principal: ${it?.principal}") }
      .map { when (it) {
        is UsernamePasswordAuthenticationToken -> (it.principal as UserDetailsWithId)
        else -> throw IllegalStateException("Unexpected Authentication type")
      }}
      .awaitSingle()
    return user;
  }

  suspend fun getUserAccessKeys(): List<String> {
    return ReactiveSecurityContextHolder.getContext()
      .mapNotNull { it?.authentication }
      .map { when (it) {
        is UsernamePasswordAuthenticationToken -> {
          val userId = (it.principal as UserDetailsWithId).id
          log.debug("The current principal: ${it.principal}")
          it.authorities.map { auth -> auth.authority } + userId
        }
        else -> listOf(ANONYMOUS_USER_ID)
      }}
      .awaitSingle()
  }
}
