package ventures.dxv.base.user.error

import org.axonframework.commandhandling.CommandExecutionException
import org.axonframework.messaging.interceptors.ExceptionHandler
import org.axonframework.queryhandling.QueryExecutionException
import ventures.dvx.base.user.api.UserError

interface UserCommandErrorSupport {
  // Doesn't get called on aggregate creation: https://github.com/AxonFramework/AxonFramework/issues/1850
  @ExceptionHandler(resultType = UserException::class)
  fun handle(ex: UserException) {
    throw UserCommandException(ex.userError.msg, ex.userError, ex)
  }
}

interface UserQueryErrorSupport {
  // Doesn't get called on aggregate creation: https://github.com/AxonFramework/AxonFramework/issues/1850
  @ExceptionHandler(resultType = UserException::class)
  fun handle(ex: UserException) {
    throw UserQueryException(ex.userError.msg, ex.userError, ex)
  }
}

object RemoteErrorMapper {
  @Suppress("ThrowableNotThrown")
  fun mapRemoteException(ex: Throwable): Throwable =
    when (ex) {
      is CommandExecutionException ->
        ex.getDetails<UserError>()
          .map<CommandExecutionException> { UserCommandException(it.msg, it) }
          .orElse(ex)
      is QueryExecutionException ->
        ex.getDetails<UserError>()
          .map<QueryExecutionException> { UserQueryException(it.msg, it) }
          .orElse(ex)
      else -> ex
    }
}
