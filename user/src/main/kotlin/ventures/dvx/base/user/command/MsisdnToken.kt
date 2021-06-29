package ventures.dvx.base.user.command

import ventures.dvx.common.types.EmailAddress
import ventures.dvx.common.types.Msisdn
import ventures.dvx.common.types.NonEmptyString
import java.time.Instant
import java.util.*

data class MsisdnToken(
  val token: String,
  val msisdn: Msisdn,
  val email: EmailAddress,
  val expires: Instant,
) {
  fun isTokenValid(): Boolean = expires.isAfter(Instant.now())

  fun matches(token: NonEmptyString, msisdn: Msisdn) =
    this.token == token.value
      && this.msisdn.value == msisdn.value

  companion object {
    val rand = Random()
    fun generateToken() = String.format("%04d", rand.nextInt(9999))
  }
}
