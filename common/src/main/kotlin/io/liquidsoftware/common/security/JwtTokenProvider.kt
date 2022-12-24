package io.liquidsoftware.common.security

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import io.liquidsoftware.common.logging.LoggerDelegate
import jakarta.annotation.PostConstruct
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.User
import java.util.*
import javax.crypto.SecretKey

open class JwtTokenProvider(
  private val jwtProperties: JwtProperties,
) {

  private val logger by LoggerDelegate()

  private var secretKey: SecretKey? = null

  companion object {
    private const val AUTHORITIES_KEY = "roles"
  }

  @PostConstruct
  fun init() {
    val secret: String = Base64.getEncoder().encodeToString(jwtProperties.secretKey.toByteArray())
    secretKey = Keys.hmacShaKeyFor(secret.toByteArray())
  }

  fun createToken(userId: String, authorities: Collection<GrantedAuthority>): String {
    val claims: Claims = Jwts.claims().setSubject(userId)
    claims[AUTHORITIES_KEY] = authorities.joinToString(",") {
        obj: GrantedAuthority -> obj.authority
    }
    val now = Date()
    val validity = Date(now.time + jwtProperties.validityInMs)
    return Jwts.builder() //
      .setClaims(claims) //
      .setIssuedAt(now) //
      .setExpiration(validity) //
      .signWith(secretKey, SignatureAlgorithm.HS256) //
      .compact()
  }

  fun createToken(authentication: Authentication): String {
    val userDto = authentication.principal as UserDetailsWithId
    val authorities: Collection<GrantedAuthority> = authentication.authorities
    val claims: Claims = Jwts.claims().setSubject(userDto.id)
    claims[AUTHORITIES_KEY] = authorities.joinToString(",") {
        obj: GrantedAuthority -> obj.authority
    }
    val now = Date()
    val validity = Date(now.time + jwtProperties.validityInMs)
    return Jwts.builder()
      .setClaims(claims)
      .setIssuedAt(now)
      .setExpiration(validity)
      .signWith(secretKey, SignatureAlgorithm.HS256)
      .compact()
  }

  fun getAuthentication(token: String): Authentication {
    val claims: Claims = Jwts.parserBuilder()
      .setSigningKey(secretKey)
      .build()
      .parseClaimsJws(token)
      .body
    val authorities: Collection<GrantedAuthority> = AuthorityUtils.commaSeparatedStringToAuthorityList(
      claims[AUTHORITIES_KEY].toString()
    )
    val principal = UserDetailsWithId(claims.subject, User(claims.subject, "", authorities))
    return UsernamePasswordAuthenticationToken(principal, token, authorities)
  }

  fun validateToken(token: String): Boolean {
    try {
      val claims: Jws<Claims> = Jwts.parserBuilder()
        .setSigningKey(secretKey)
        .build()
        .parseClaimsJws(token)
      return !claims.body.expiration.before(Date())
    } catch (e: JwtException) {
      logger.info("Invalid JWT token.")
      logger.trace("Invalid JWT token trace.", e)
    } catch (e: IllegalArgumentException) {
      logger.info("Invalid JWT token.")
      logger.trace("Invalid JWT token trace.", e)
    }
    return false
  }

}
