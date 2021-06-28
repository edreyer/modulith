package ventures.dvx.common.axon.security

import org.axonframework.commandhandling.CommandMessage
import org.axonframework.messaging.MessageDispatchInterceptor
import org.axonframework.messaging.MetaData
import org.springframework.security.core.context.SecurityContextHolder
import java.util.function.BiFunction

class AccessControlDispatchInterceptor : MessageDispatchInterceptor<CommandMessage<*>> {

  // Adds principal to the message metadata for Access checking later
  override fun handle(messages: MutableList<out CommandMessage<*>>)
  : BiFunction<Int, CommandMessage<*>, CommandMessage<*>> = BiFunction { i, msg ->
    SecurityContextHolder.getContext()
      ?.authentication
      ?.let { msg.withMetaData(MetaData.with("authentication", it)) }
      ?: msg
  }
}
