package ventures.dvx.bridgekeeper

import org.axonframework.commandhandling.CommandMessage
import org.axonframework.messaging.InterceptorChain
import org.axonframework.messaging.annotation.MetaDataValue
import org.axonframework.messaging.interceptors.MessageHandlerInterceptor
import org.springframework.security.access.annotation.Secured

interface AccessControlCommandSupport {

  fun establishResourceType(party: Party): ResourceType

  fun getId(): String

  @MessageHandlerInterceptor(messageType = CommandMessage::class)
  fun checkCommandAuthorization(
    cmdMsg: CommandMessage<*>,
    chain: InterceptorChain,
    bridgeKeeper: BridgeKeeper,
    @MetaDataValue("party") party: Party
  ): Any = when (cmdMsg.payload) {
    is Secured -> {
      val userRt = establishResourceType(party)
      bridgeKeeper.assertCanPerform(party, userRt, cmdMsg.commandName)
        .orElseThrow { AccessControlCommandException(getId(), party.id, cmdMsg.commandName) }
      chain.proceed()
    }
    else -> chain.proceed()
  }

}
