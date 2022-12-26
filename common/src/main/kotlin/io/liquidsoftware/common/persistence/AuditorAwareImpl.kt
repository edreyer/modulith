package io.liquidsoftware.common.persistence

import org.springframework.data.domain.ReactiveAuditorAware
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import reactor.core.publisher.Mono

class AuditorAwareImpl : ReactiveAuditorAware<String> {
  override fun getCurrentAuditor(): Mono<String> =
    Mono
      .just(SecurityContextHolder.getContext())
      .map(SecurityContext::getAuthentication)
      .filter(Authentication::isAuthenticated)
      .map(Authentication::getName)

}
