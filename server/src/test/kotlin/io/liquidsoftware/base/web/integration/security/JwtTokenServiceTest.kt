package io.liquidsoftware.base.web.integration.security

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.Jwts.SIG.HS256
import io.jsonwebtoken.security.Keys
import io.liquidsoftware.common.security.JwtProperties
import io.liquidsoftware.common.security.JwtTokenService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.User
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Date

internal class JwtTokenServiceTest {
  private var log = LoggerFactory.getLogger(JwtTokenServiceTest::class.java)
  private lateinit var jwtTokenService: JwtTokenService
  private lateinit var properties: JwtProperties

  @BeforeEach
  fun setup() {
    properties = JwtProperties()
    log.debug("jwt properties::{}", properties)
    jwtTokenService = JwtTokenService(properties)
    Assertions.assertNotNull(jwtTokenService)
  }

  @Test
  fun testGenerateAndParseToken() {
    val authorities: Collection<GrantedAuthority> = AuthorityUtils.createAuthorityList(listOf(TEST_ROLE_NAME))
    val token = jwtTokenService.generateToken(TEST_USERNAME, authorities)
    log.debug("generated jwt token::$token")

    val userDetails = User(TEST_USERNAME, "password", authorities)
    val valid = jwtTokenService.validateToken(token, userDetails)
    assertThat(valid).isTrue()

    assertThat(jwtTokenService.extractUsername(token)).isEqualTo(TEST_USERNAME)
  }

  @Test
  fun testGenerateAndParseToken_withoutRoles() {
    val token = jwtTokenService.generateToken(TEST_USERNAME, emptyList())
    log.debug("generated jwt token::$token")
    val auth = jwtTokenService.extractUsername(token)
    assertThat(auth).isEqualTo(TEST_USERNAME)
  }

  @Test
  fun testParseTokenException() {
    val token = "anunknowtokencannotbeparsedbyjwtprovider"
    val authorities: Collection<GrantedAuthority> = AuthorityUtils.createAuthorityList(listOf(TEST_ROLE_NAME))
    val userDetails = User(TEST_USERNAME, "password", authorities)
    assertThat(jwtTokenService.validateToken(token, userDetails)).isFalse()
  }

  @Test
  fun testExpired() {
    val secret = Base64.getEncoder().encodeToString(properties.secretKey.toByteArray())
    val secretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    val claims: Claims = Jwts.claims().subject(TEST_USERNAME).build()
    val now = Date()
    val validity = Date(now.time - 1)
    val expiredToken = Jwts.builder()
      .claims(claims)
      .issuedAt(now)
      .expiration(validity)
      .signWith(secretKey, HS256)
      .compact()
    val authorities: Collection<GrantedAuthority> = AuthorityUtils.createAuthorityList(listOf(TEST_ROLE_NAME))
    val userDetails = User(TEST_USERNAME, "password", authorities)
    assertThat(jwtTokenService.validateToken(expiredToken, userDetails)).isFalse()
  }

  @Test
  fun testValidateTokenException() {
    val authorities: Collection<GrantedAuthority> = AuthorityUtils.createAuthorityList(listOf(TEST_ROLE_NAME))
    val token = jwtTokenService.generateToken(TEST_USERNAME, authorities)
    val userDetails = User(TEST_USERNAME, "password", authorities)
    assertThat(jwtTokenService.validateToken(token, userDetails)).isTrue()
  }

  companion object {
    private const val TEST_USERNAME = "username"
    private const val TEST_ROLE_NAME = "ROLE_USER"
  }
}
