package ventures.dvx.common.axon.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ventures.dvx.bridgekeeper.RoleHandle

val ROLE_SYSTEM = object : RoleHandle("ROLE_SYSTEM") {}

private fun getSystemAuthentication(): UsernamePasswordAuthenticationToken {
  val userDetails: UserDetails = User("SYSTEM", "", listOf(SimpleGrantedAuthority(ROLE_SYSTEM.name)))
  return UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
}

fun <T> Mono<T>.runAsSuperUser(): Mono<T> =
  this.contextWrite(ReactiveSecurityContextHolder.withAuthentication(getSystemAuthentication()))

fun <T> Flux<T>.runAsSuperUser(): Flux<T> =
  this.contextWrite(ReactiveSecurityContextHolder.withAuthentication(getSystemAuthentication()))
