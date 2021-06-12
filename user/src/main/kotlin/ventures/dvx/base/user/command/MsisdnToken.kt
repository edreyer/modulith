package ventures.dvx.base.user.command

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle.apply
import org.axonframework.spring.stereotype.Aggregate
import ventures.dvx.base.user.api.CreateTokenCommand
import ventures.dvx.base.user.api.MsisdnTokenId
import ventures.dvx.base.user.api.TokenCreatedEvent
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

@Aggregate
class MsisdnToken {

  @AggregateIdentifier
  lateinit var id: MsisdnTokenId

  private lateinit var token: String
  private lateinit var msisdn: String
  private lateinit var email: String
  private lateinit var expires: Instant
  private var used: Boolean = false

  @CommandHandler
  constructor(command: CreateTokenCommand, clock: Clock) {
    // TODO generate token
    val token = "1234"

    apply(TokenCreatedEvent(
      id = MsisdnTokenId(),
      msisdn = command.msisdn,
      email = command.email,
      token = token,
      expires = clock.instant().plus(1, ChronoUnit.HOURS)
    ))
  }

  @EventSourcingHandler
  private fun on(event: TokenCreatedEvent) {
    id = event.id

    token = event.token
    msisdn = event.msisdn
    email = event.email
    expires = event.expires
    used = false
  }

  fun isTokenValid(): Boolean = !used && expires.isAfter(Instant.now())

}
