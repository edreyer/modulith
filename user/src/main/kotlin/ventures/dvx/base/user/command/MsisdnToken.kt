package ventures.dvx.base.user.command

import java.time.Instant
import java.util.*

data class MsisdnToken(
  val token: String,
  val msisdn: String,
  val email: String,
  val expires: Instant,
) {
  fun isTokenValid(): Boolean = expires.isAfter(Instant.now())

  fun matches(token: String, msisdn: String) =
    this.token == token
      && this.msisdn == msisdn

  companion object {
    val rand = Random()
    fun generateToken() = String.format("%04d", rand.nextInt(9999))
  }
}
