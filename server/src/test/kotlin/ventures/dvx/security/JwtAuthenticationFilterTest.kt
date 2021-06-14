package ventures.dvx.security

import com.ninjasquad.springmockk.MockkClear
import com.ninjasquad.springmockk.clear
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import ventures.dvx.common.security.JwtTokenAuthenticationFilter
import ventures.dvx.common.security.JwtTokenProvider


internal class JwtAuthenticationFilterTest {
  private val tokenProvider = mockk<JwtTokenProvider>()
  private val exchange = mockk<ServerWebExchange>()
  private val chain = mockk<WebFilterChain>()

  @BeforeEach
  private fun setup() {
    tokenProvider.clear(MockkClear.BEFORE)
    exchange.clear(MockkClear.BEFORE)
    chain.clear(MockkClear.BEFORE)
  }

  @Test
  fun testFilter() {
    val filter = JwtTokenAuthenticationFilter(tokenProvider)
    val usernamePasswordToken = UsernamePasswordAuthenticationToken(
      "test", "password",
      AuthorityUtils.createAuthorityList("ROLE_USER")
    )
    every { exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION) } returns "Bearer atesttoken"
    every { tokenProvider.validateToken(any()) } returns true
    every { tokenProvider.getAuthentication(any())} returns usernamePasswordToken
    every { chain.filter(exchange) } returns Mono.empty()
    filter.filter(exchange, chain)
    verify(exactly = 1) { chain.filter(exchange) }
  }

  @Test
  fun testFilterWithNoToken() {
    val filter = JwtTokenAuthenticationFilter(tokenProvider)
    every { exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION) } returns null
    every { chain.filter(exchange) } returns Mono.empty()
    filter.filter(exchange, chain)
    verify(exactly = 1) { chain.filter(exchange) }
  }

  @Test
  fun testFilterWithInvalidToken() {
    val filter = JwtTokenAuthenticationFilter(tokenProvider)
    every { exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION) } returns "Bearer atesttoken"
    every { tokenProvider.validateToken(any()) } returns false
    every { chain.filter(exchange) } returns Mono.empty()
    filter.filter(exchange, chain)
    verify(exactly = 1) { chain.filter(exchange) }
  }
}
