package io.liquidsoftware.common.security

import arrow.core.Option
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter


@Component
class JwtTokenAuthenticationFilter(
  private val tokenService: JwtTokenService,
  private val userDetailsService: UserDetailsService
) : OncePerRequestFilter() {

  companion object {
    const val HEADER_PREFIX = "Bearer "
  }

  override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
    val token = resolveToken(request)
      token?.let { tokenService.extractUsername(it) }
      ?.takeIf { SecurityContextHolder.getContext().authentication == null }
      ?.also { username ->
        val userDetails = userDetailsService.loadUserByUsername(username)
        if (tokenService.validateToken(token, userDetails)) {
          val authToken = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
          authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
          SecurityContextHolder.getContext().authentication = authToken
        }
      }
    chain.doFilter(request, response);
  }

  private fun resolveToken(request: HttpServletRequest): String? {
    return Option.fromNullable(request .getHeader(HttpHeaders.AUTHORIZATION))
      .filter { it.startsWith(HEADER_PREFIX) }
      .map { it.substring(7) }
      .getOrNull()
  }

}
