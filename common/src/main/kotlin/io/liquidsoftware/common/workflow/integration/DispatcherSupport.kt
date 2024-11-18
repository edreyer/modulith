package io.liquidsoftware.common.workflow.integration

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.SecurityCoroutineContext
import io.liquidsoftware.common.workflow.Command
import io.liquidsoftware.common.workflow.Event
import io.liquidsoftware.common.workflow.MissingHandler
import io.liquidsoftware.common.workflow.Query
import io.liquidsoftware.common.workflow.Request
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.integration.annotation.MessageEndpoint
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.messaging.handler.annotation.Payload

interface RequestGateway {
  fun <W: WorkflowError, E: Event>send(@Payload request: Request): Either<W, E>
}

// Message Handler using Registry
@MessageEndpoint
class RegistryAwareRequestHandler {
  private val logger by LoggerDelegate()

  private suspend fun handleCommand(command: Command) = either {
    ensureNotNull(WorkflowRegistry.commandHandlers[command::class]) {
      MissingHandler("No handler for command $command")
    }.invoke(command)
  }

  private suspend fun handleQuery(query: Query) = either {
    ensureNotNull(WorkflowRegistry.queryHandlers[query::class]) {
      MissingHandler("No handler for query $query")
    }.invoke(query)
  }

  @ServiceActivator(
    inputChannel = "requestChannel",
    outputChannel = "replyChannel"
  )
  fun handle(request: Request): Either<WorkflowError, Event> {
    logger.debug("Handling request of type: ${request::class.simpleName}")

    return runBlocking(context = Dispatchers.Default + SecurityCoroutineContext()) {
      when (request) {
        is Command -> async { handleCommand(request) }
        is Query -> async { handleQuery(request) }
      }.await()
    }
  }
}
