package io.liquidsoftware.common.workflow

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.SecurityCoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

/*
 Inspired by: https://medium.com/swlh/cqrs-and-application-pipelines-in-kotlin-441d8f7fe427
 */
@Component
class WorkflowDispatcher {
  val log by LoggerDelegate()

  suspend fun <E: Event> dispatch(query: Query): Either<WorkflowError, E> =
    withContext(Dispatchers.Default + SecurityCoroutineContext()) {
      either {
        ensureNotNull(WorkflowRegistry.queryHandlers[query::class]) {
          MissingHandler("No handler for query $query")
        }.let {
          it.invoke(query) as E
        }
      }
    }


  suspend fun <E: Event> dispatch(command: Command): Either<WorkflowError, E> =
    withContext(Dispatchers.Default + SecurityCoroutineContext()) {
      either {
        ensureNotNull(WorkflowRegistry.commandHandlers[command::class]) {
          MissingHandler("No handler for command $command")
        }.let {
          it.invoke(command) as E
        }
      }
    }

}
