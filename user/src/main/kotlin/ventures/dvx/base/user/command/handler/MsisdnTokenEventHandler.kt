package ventures.dvx.base.user.command.handler

import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component
import ventures.dvx.base.user.api.TokenCreatedEvent
import ventures.dvx.common.logging.LoggerDelegate

@Component
class MsisdnTokenEventHandler {

  val logger by LoggerDelegate()

  @EventHandler
  fun handle(event: TokenCreatedEvent) {
    logger.trace("Send '${event.token}' token via SMS to ${event.msisdn}")
    // TODO("Send ${event.token} token via SMS to ${event.msisdn}")
  }
}
