package io.liquidsoftware.common.persistence

import org.springframework.data.domain.AuditorAware
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional


class AuditorAwareImpl : AuditorAware<String> {
  override fun getCurrentAuditor(): Optional<String> =
    Optional.ofNullable(SecurityContextHolder.getContext())
      .map { it.authentication }
      .filter { it.isAuthenticated }
      .map(Authentication::getName)

}
