package io.liquidsoftware.common.workflow

import arrow.core.Either
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.workflow.integration.RequestGateway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/*
 Inspired by: https://medium.com/swlh/cqrs-and-application-pipelines-in-kotlin-441d8f7fe427
 */
@Component
class WorkflowDispatcher(
  @Autowired val gateway: RequestGateway
) {
  val log by LoggerDelegate()

  suspend fun <E: Event> dispatch(query: Query): Either<WorkflowError, E> = gateway.send(query)

  suspend fun <E: Event> dispatch(command: Command): Either<WorkflowError, E> = gateway.send(command)

}
