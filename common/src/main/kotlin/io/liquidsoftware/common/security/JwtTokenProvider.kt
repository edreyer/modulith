package io.liquidsoftware.common.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.Jwts.SIG.HS256
import io.jsonwebtoken.security.Keys
import io.liquidsoftware.common.logging.LoggerDelegate
import jakarta.annotation.PostConstruct
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.User
import java.util.Base64
import java.util.Date
import javax.crypto.SecretKey

open class JwtTokenProvider(
  private val jwtProperties: JwtProperties,
) {

  private val logger by LoggerDelegate()

  private val secretKey: SecretKey
    get() {
      val secret: String = Base64.getEncoder().encodeToString(jwtProperties.secretKey.toByteArray())
      return Keys.hmacShaKeyFor(secret.toByteArray())
    }

  companion object {
    private const val AUTHORITIES_KEY = "roles"
  }

  fun createToken(userId: String, authorities: Collection<GrantedAuthority>): String {
    val claims: Claims = Jwts.claims().subject(userId).build()
    claims[AUTHORITIES_KEY] = authorities.joinToString(",") {
        obj: GrantedAuthority -> obj.authority
    }
    val now = Date()
    val validity = Date(now.time + jwtProperties.validityInMs)
    return Jwts.builder() //
      .claims(claims) //
      .issuedAt(now) //
      .expiration(validity) //
      .signWith(secretKey, HS256) //
      .compact()
  }

  fun createToken(authentication: Authentication): String {
    val userDto = authentication.principal as UserDetailsWithId
    val authorities: Collection<GrantedAuthority> = authentication.authorities
    val claims: Claims = Jwts.claims().subject(userDto.id)
      .add(AUTHORITIES_KEY, authorities.joinToString(",") {
          obj: GrantedAuthority -> obj.authority
      })
      .build()
    val now = Date()
    val validity = Date(now.time + jwtProperties.validityInMs)
    return Jwts.builder()
      .claims(claims)
      .issuedAt(now)
      .expiration(validity)
      .signWith(secretKey, HS256)
      .compact()
  }

  fun getAuthentication(token: String): Authentication {
    val claims: Claims = Jwts
      .parser()
      .verifyWith(secretKey)
      .build()
      .parseSignedClaims(token)
      .payload
    val authorities: Collection<GrantedAuthority> = AuthorityUtils.commaSeparatedStringToAuthorityList(
      claims[AUTHORITIES_KEY].toString()
    )
    val principal = UserDetailsWithId(claims.subject, User(claims.subject, "", authorities))
    return UsernamePasswordAuthenticationToken(principal, token, authorities)
  }

  fun validateToken(token: String): Boolean {
    try {
      val claims: Jws<Claims> = Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
      return !claims.payload.expiration.before(Date())
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
