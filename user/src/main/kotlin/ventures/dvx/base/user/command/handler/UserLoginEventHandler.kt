package ventures.dvx.base.user.command.handler

import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component
import ventures.dvx.base.user.api.EndUserLoginStartedEvent
import ventures.dvx.base.user.api.UserRegistrationStartedEvent
import ventures.dvx.common.service.sms.SmsService

@Component
class UserLoginEventHandler {

  @EventHandler
  fun handle(event: EndUserLoginStartedEvent, smsService: SmsService) =
    smsService.sendMessage(
      event.token.msisdn.value,
      "Here is your DVX Application Token: ${event.token.token}"
    )

  @EventHandler
  fun handle(event: UserRegistrationStartedEvent, smsService: SmsService) =
    smsService.sendMessage(
      event.token.msisdn.value,
      "Here is your DVX Application Token: ${event.token.token}"
    )

}
