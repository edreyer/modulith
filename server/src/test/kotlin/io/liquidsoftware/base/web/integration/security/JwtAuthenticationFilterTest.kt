package io.liquidsoftware.base.web.integration.security

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.liquidsoftware.common.security.JwtProperties
import io.liquidsoftware.common.security.JwtTokenAuthenticationFilter
import io.liquidsoftware.common.security.JwtTokenService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService

internal class JwtAuthenticationFilterTest {
  private val username = "test"
  private val tokenService = JwtTokenService(JwtProperties())

  @BeforeEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @AfterEach
  fun cleanupSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun testFilter() {
    val userDetails = User(username, "password", emptyList())
    val request = requestWithToken(tokenFor(userDetails))
    val response = MockHttpServletResponse()
    val chain = MockFilterChain()
    val filter = JwtTokenAuthenticationFilter(tokenService, FixedUserDetailsService(userDetails))

    filter.doFilter(request, response, chain)

    assertThat(chain.request).isNotNull()
    assertThat(SecurityContextHolder.getContext().authentication).isNotNull()
    assertThat(SecurityContextHolder.getContext().authentication.name).isEqualTo(username)
  }

  @Test
  fun testFilterWithNoToken() {
    val request = MockHttpServletRequest()
    val response = MockHttpServletResponse()
    val chain = MockFilterChain()
    val filter = JwtTokenAuthenticationFilter(tokenService, ThrowingUserDetailsService())

    filter.doFilter(request, response, chain)

    assertThat(chain.request).isNotNull()
    assertThat(SecurityContextHolder.getContext().authentication).isNull()
  }

  @Test
  fun testFilterWithMalformedAuthorizationHeader() {
    val request = MockHttpServletRequest().apply {
      addHeader(HttpHeaders.AUTHORIZATION, "Token not-a-bearer-token")
    }
    val response = MockHttpServletResponse()
    val chain = MockFilterChain()
    val filter = JwtTokenAuthenticationFilter(tokenService, ThrowingUserDetailsService())

    filter.doFilter(request, response, chain)

    assertThat(chain.request).isNotNull()
    assertThat(SecurityContextHolder.getContext().authentication).isNull()
  }

  private fun tokenFor(userDetails: UserDetails): String =
    tokenService.generateToken(userDetails.username, userDetails.authorities)

  private fun requestWithToken(token: String) = MockHttpServletRequest().apply {
    addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
  }

  private class FixedUserDetailsService(
    private val userDetails: UserDetails
  ) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails = userDetails
  }

  private class ThrowingUserDetailsService : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
      error("UserDetailsService should not be called when no token is present")
    }
  }
}
