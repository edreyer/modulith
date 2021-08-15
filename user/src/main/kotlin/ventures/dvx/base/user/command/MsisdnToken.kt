package ventures.dvx.base.user.command

import ventures.dvx.common.types.EmailAddress
import ventures.dvx.common.types.Msisdn
import ventures.dvx.common.types.NonEmptyString
import java.security.SecureRandom
import java.time.Instant

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
    private val rand = SecureRandom.getInstance("NativePRNG")
    fun generateToken() = String.format("%04d", rand.nextInt(9999))
  }
}
