package ventures.dvx.base.user.command

import java.time.Instant

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

}
