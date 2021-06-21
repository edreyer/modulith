package ventures.dvx.base.user.web

import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dxv.base.user.error.UserCommandException
import ventures.dxv.base.user.error.UserException
import ventures.dxv.base.user.error.UserQueryException

abstract class BaseUserController {

  val log by LoggerDelegate()

  /**
   * Extension method to translate Monos in an exceptional state
   */
  fun Mono<ResponseEntity<OutputDto>>.mapToResponseEntity(): Mono<ResponseEntity<OutputDto>> {
    return this.onErrorResume { ex ->
      log.error(ex.message, ex)
      when (ex) {
        is UserException -> Mono.just(ResponseEntity
          .status(ex.userError.responseStatus)
          .body(OutputErrorDto(ex.message) as OutputDto)
        )
        is UserCommandException -> Mono.just(ResponseEntity
          .status(ex.details.responseStatus)
          .body(OutputErrorDto(ex.message) as OutputDto)
        )
        is UserQueryException -> Mono.just(ResponseEntity
          .status(ex.details.responseStatus)
          .body(OutputErrorDto(ex.message) as OutputDto)
        )
        else -> Mono.just(ResponseEntity
          .badRequest()
          .body(OutputErrorDto(ex.message ?: "Server Error") as OutputDto)
        )
      }
    }
  }
}

