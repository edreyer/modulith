package ventures.dvx.base.user.command.interceptor

import org.axonframework.commandhandling.CommandMessage
import org.axonframework.messaging.MessageDispatchInterceptor
import org.axonframework.messaging.interceptors.ExceptionHandler
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dxv.base.user.error.UserCommandException
import ventures.dxv.base.user.error.UserException
import java.util.function.BiFunction

// disable interceptor for now
//@Component
class UserCreationDispatchInterceptor(
  private val indexRepository: IndexRepository
) : MessageDispatchInterceptor<CommandMessage<*>> {

  override fun handle(messages: MutableList<out CommandMessage<*>>?)
  : BiFunction<Int, CommandMessage<*>, CommandMessage<*>> {
    return BiFunction { i, msg ->
      msg
//      when (val command = msg.payload) {
//        is RegisterAdminUserCommand -> {
//          indexRepository.findEntityByAggregateNameAndKey(AdminUser.aggregateName(), command.email)
//            ?.let { throw UserException(AdminUserExistsError(command.email)) }
//          msg
//        }
//        is RegisterEndUserCommand -> {
//          indexRepository.findEntityByAggregateNameAndKey(EndUser.aggregateName(), command.msisdn)
//            ?.let { throw UserException(EndUserExistsError(command.msisdn)) }
//          msg
//        }
//        else -> msg
//      }
    }
  }

  // Doesn't get called on aggregate creation: https://github.com/AxonFramework/AxonFramework/issues/1850
  @ExceptionHandler(resultType = UserException::class)
  fun handle(ex: UserException) {
    throw UserCommandException(ex.userError.msg, ex.userError, ex)
  }

}
