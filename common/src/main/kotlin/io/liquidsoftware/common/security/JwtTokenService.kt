package io.liquidsoftware.common.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.Jwts.SIG.HS256
import io.jsonwebtoken.security.Keys
import io.liquidsoftware.common.logging.LoggerDelegate
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.Base64
import java.util.Date
import javax.crypto.SecretKey


open class JwtTokenService(
  private val jwtProperties: JwtProperties,
) {

  private val logger by LoggerDelegate()

  private val secretKey: SecretKey by lazy {
    val secret: String = Base64.getEncoder().encodeToString(jwtProperties.secretKey.toByteArray())
    Keys.hmacShaKeyFor(secret.toByteArray())
  }

  companion object {
    private const val AUTHORITIES_KEY = "roles"
  }

  fun generateToken(username: String, authorities: Collection<GrantedAuthority>): String {
    val claims: Claims = Jwts
      .claims()
      .add(AUTHORITIES_KEY, authorities.joinToString(",") {
          obj: GrantedAuthority -> obj.authority
      })
      .subject(username)
      .build()

    return createToken(claims)
  }

  private fun createToken(claims: Map<String, Any?>): String {
    return Jwts.builder()
      .claims(claims)
      .issuedAt(Date(System.currentTimeMillis())) //
      .expiration(Date(System.currentTimeMillis() + 1000 * 60 * 30)) //
      .signWith(secretKey, HS256)
      .compact()
  }

  fun extractUsername(token: String): String {
    return extractClaim(token) { obj: Claims -> obj.subject }
  }

  private fun extractExpiration(token: String): Date {
    return extractClaim(token) { obj: Claims -> obj.expiration }
  }

  private fun isTokenExpired(token: String): Boolean {
    return extractExpiration(token).before(Date())
  }

  fun validateToken(token: String, userDetails: UserDetails): Boolean = Result.runCatching {
    val username = extractUsername(token)
    (username == userDetails.username && !isTokenExpired(token))
  }.fold( { true }, {  false } )


  private fun <T> extractClaim(token: String, claimsResolver: (Claims) -> T): T {
    val claims = extractAllClaims(token)
    return claimsResolver(claims)
  }

  private fun extractAllClaims(token: String): Claims {
    return Jwts.parser()
      .verifyWith(secretKey)
      .build()
      .parseSignedClaims(token)
      .payload
  }

}
