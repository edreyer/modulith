package ventures.dvx.base.user.query

import org.axonframework.eventhandling.EventHandler
import org.axonframework.queryhandling.QueryHandler
import org.springframework.stereotype.Component
import ventures.dvx.base.user.api.FindMsisdnToken
import ventures.dvx.base.user.api.TokenCreatedEvent

@Component
class MsisdnTokenProjector {

  @EventHandler
  private fun on(event: TokenCreatedEvent, msisdnTokenViewRepository: MsisdnTokenViewRepository) {
    val msisdnTokenView = MsisdnTokenView(
      id = event.id.id,
      msisdn = event.msisdn,
      email = event.email,
      token = event.token,
      expires = event.expires,
      used = false
    )
    msisdnTokenViewRepository.save(msisdnTokenView);
  }

  @QueryHandler
  private fun handle(query: FindMsisdnToken, msisdnTokenViewRepository: MsisdnTokenViewRepository): MsisdnTokenView? {
    val token = msisdnTokenViewRepository.findByMsisdnAndTokenAndUsedFalse(query.msisdn, query.token)
      .firstOrNull { it.isTokenValid() }
    return token
  }

}
