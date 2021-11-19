package ventures.dvx.base.web.integration.security

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import ventures.dvx.common.security.JwtProperties
import ventures.dvx.common.security.JwtTokenProvider
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Date
import java.util.stream.Collectors

internal class JwtTokenProviderTest {
  private var log = LoggerFactory.getLogger(JwtTokenProviderTest::class.java)
  private lateinit var jwtTokenProvider: JwtTokenProvider
  private lateinit var properties: JwtProperties

  @BeforeEach
  private fun setup() {
    properties = JwtProperties()
    log.debug("jwt properties::" + properties)
    jwtTokenProvider = JwtTokenProvider(properties)
    Assertions.assertNotNull(jwtTokenProvider)
    jwtTokenProvider.init()
  }

  @Test
  fun testGenerateAndParseToken() {
    val token = generateToken(TEST_USER, TEST_ROLE_NAME)
    log.debug("generated jwt token::$token")
    val auth = jwtTokenProvider.getAuthentication(token)
    val principal = auth.principal as UserDetails
    org.assertj.core.api.Assertions.assertThat(principal.username).isEqualTo(TEST_USER)
    org.assertj.core.api.Assertions.assertThat(
      principal.authorities.stream()
        .map { obj: GrantedAuthority -> obj.authority }
        .collect(Collectors.toList())
    ).contains(TEST_ROLE_NAME)
  }

  @Test
  fun testGenerateAndParseToken_withoutRoles() {
    val token = generateToken(TEST_USER)
    log.debug("generated jwt token::$token")
    val auth = jwtTokenProvider.getAuthentication(token)
    val principal = auth.principal as UserDetails
    org.assertj.core.api.Assertions.assertThat(principal.username).isEqualTo(TEST_USER)
    org.assertj.core.api.Assertions.assertThat(principal.authorities).isEmpty()
  }

  @Test
  fun testParseTokenException() {
    val token = "anunknowtokencannotbeparsedbyjwtprovider"
    Assertions.assertThrows(JwtException::class.java) {
      jwtTokenProvider.getAuthentication(
        token
      )
    }
    org.assertj.core.api.Assertions.assertThatThrownBy {
      jwtTokenProvider.getAuthentication(
        token
      )
    }.isInstanceOf(
      JwtException::class.java
    )
  }

  @Test
  fun testValidateTokenException_failed() {
    val token = "anunknowtokencannotbeparsedbyjwtprovider"
    org.assertj.core.api.Assertions.assertThat(jwtTokenProvider.validateToken(token)).isFalse
  }

  @Test
  fun testValidateExpirationDate() {
    val secret = Base64.getEncoder().encodeToString(properties.secretKey.toByteArray())
    val secretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    val claims = Jwts.claims().setSubject(TEST_USER)
    val now = Date()
    val validity = Date(now.time - 1)
    val expiredToken = Jwts.builder()
      .setClaims(claims)
      .setIssuedAt(now)
      .setExpiration(validity)
      .signWith(secretKey, SignatureAlgorithm.HS256)
      .compact()
    org.assertj.core.api.Assertions.assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse
  }

  @Test
  fun testValidateTokenException() {
    val token = generateToken(TEST_USER, TEST_ROLE_NAME)
    org.assertj.core.api.Assertions.assertThat(jwtTokenProvider.validateToken(token)).isTrue
  }

  private fun generateToken(username: String, vararg roles: String): String {
    val authorities: Collection<GrantedAuthority> = AuthorityUtils.createAuthorityList(*roles)
    val principal = User(username, "password", authorities)
    val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken(principal, null, authorities)
    return jwtTokenProvider.createToken(usernamePasswordAuthenticationToken)
  }

  companion object {
    private const val TEST_USER = "user"
    private const val TEST_ROLE_NAME = "ROLE_USER"
  }
}
