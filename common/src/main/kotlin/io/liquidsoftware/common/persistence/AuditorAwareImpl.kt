package io.liquidsoftware.common.persistence

import org.springframework.data.domain.ReactiveAuditorAware
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import reactor.core.publisher.Mono


class AuditorAwareImpl : ReactiveAuditorAware<String> {
  override fun getCurrentAuditor(): Mono<String> =
    ReactiveSecurityContextHolder
      .getContext()
      .map { obj: SecurityContext -> obj.authentication }
      .filter { obj: Authentication -> obj.isAuthenticated }
      .map(Authentication::getName)

}
