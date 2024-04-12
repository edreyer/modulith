package io.liquidsoftware.base.web.integration.security

import com.ninjasquad.springmockk.MockkClear
import com.ninjasquad.springmockk.clear
import io.liquidsoftware.common.security.JwtTokenAuthenticationFilter
import io.liquidsoftware.common.security.JwtTokenService
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.DispatcherType
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService

internal class JwtAuthenticationFilterTest {
  private val tokenService = mockk<JwtTokenService>()
  private val userDetailsService = mockk<UserDetailsService>()
  private val authenticationManager = mockk<AuthenticationManager>()
  private val request = mockk<HttpServletRequest>()
  private val response = mockk<HttpServletResponse>()
  private val chain = mockk<FilterChain>()

  private val USERNAME = "test"

  @BeforeEach
  fun setup() {
    tokenService.clear(MockkClear.BEFORE)
    userDetailsService.clear(MockkClear.BEFORE)
    authenticationManager.clear(MockkClear.BEFORE)
    request.clear(MockkClear.BEFORE)
    response.clear(MockkClear.BEFORE)
    chain.clear(MockkClear.BEFORE)
  }

  @Test
  fun testFilter() {
    val filter = JwtTokenAuthenticationFilter(tokenService, userDetailsService)
    val usernamePasswordToken = UsernamePasswordAuthenticationToken(
      USERNAME, "password",
      AuthorityUtils.createAuthorityList("ROLE_USER")
    )
    every { request.getHeader(HttpHeaders.AUTHORIZATION) } returns "Bearer atesttoken"
    every { request.getAttribute(any()) } returns null
    every { request.remoteAddr } returns "127.0.0.1"
    every { request.getSession(false)} returns null
    justRun { request.setAttribute(any(), true) }
    justRun { request.removeAttribute(any()) }
    every { tokenService.extractUsername(any()) } returns USERNAME
    every { userDetailsService.loadUserByUsername(any()) } returns User(USERNAME, "password", emptyList())
    every { tokenService.validateToken(any(), any()) } returns true
    every { request.dispatcherType } returns DispatcherType.REQUEST
    justRun { chain.doFilter(any(), any()) }
    filter.doFilter(request, response, chain)
    verify(exactly = 1) { chain.doFilter(request, response) }
    verify(exactly = 1) { tokenService.validateToken(any(), any()) }
   }

  @Test
  fun testFilterWithNoToken() {
    val filter = JwtTokenAuthenticationFilter(tokenService, userDetailsService)
    every { request.getHeader(HttpHeaders.AUTHORIZATION) } returns null
    every { request.getAttribute(any()) } returns null
    justRun { request.setAttribute(any(), true) }
    justRun { request.removeAttribute(any()) }
    every { request.dispatcherType } returns DispatcherType.REQUEST
    justRun { chain.doFilter(any(), any()) }
    filter.doFilter(request, response, chain)
    verify(exactly = 1) { chain.doFilter(request, response) }
  }

  @Test
  fun testFilterWithInvalidToken() {
    val filter = JwtTokenAuthenticationFilter(tokenService, userDetailsService)
    every { request.getHeader(HttpHeaders.AUTHORIZATION) } returns "Bearer atesttoken"
    every { request.getAttribute(any()) } returns null
    justRun { request.setAttribute(any(), true) }
    every { tokenService.extractUsername(any()) } returns USERNAME
    every { userDetailsService.loadUserByUsername(any()) } returns User(USERNAME, "password", emptyList())
    every { tokenService.validateToken(any(), any()) } returns false
    justRun { request.removeAttribute(any()) }
    every { request.dispatcherType } returns DispatcherType.REQUEST
    justRun { chain.doFilter(any(), any()) }
    filter.doFilter(request, response, chain)
    verify(exactly = 1) { chain.doFilter(request, response) }
    verify(exactly = 1) { tokenService.validateToken(any(), any()) }
  }
}
