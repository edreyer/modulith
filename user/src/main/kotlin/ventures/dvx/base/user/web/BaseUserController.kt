package ventures.dvx.base.user.web

import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dxv.base.user.error.UserCommandException
import ventures.dxv.base.user.error.UserException
import ventures.dxv.base.user.error.UserQueryException

abstract class BaseUserController {

  private val log by LoggerDelegate()

  /**
   * Extension method to translate both success and error responses
   */
  fun <T> Mono<T>.mapToResponse(onSuccess: (T) -> ResponseEntity<OutputDto>)
  : Mono<ResponseEntity<OutputDto>> {
    return this
      .map { onSuccess(it) }
      .onErrorResume { ex ->
        log.error(ex.message, ex)
        when (ex) {
          is UserException -> Mono.just(ResponseEntity
            .status(ex.userError.responseStatus)
            .body(OutputErrorDto(ex.message))
          )
          is UserCommandException -> Mono.just(ResponseEntity
            .status(ex.details.responseStatus)
            .body(OutputErrorDto(ex.message))
          )
          is UserQueryException -> Mono.just(ResponseEntity
            .status(ex.details.responseStatus)
            .body(OutputErrorDto(ex.message))
          )
          else -> Mono.just(ResponseEntity
            .internalServerError()
            .body(OutputErrorDto(ex.message ?: "Server Error"))
          )
        }
      }
  }

}

