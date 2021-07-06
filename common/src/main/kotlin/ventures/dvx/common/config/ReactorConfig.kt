package ventures.dvx.common.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import reactor.core.publisher.Hooks
import reactor.tools.agent.ReactorDebugAgent

@Configuration
@Profile("!prod")
class ReactorConfig {

  init {
    // For debugging Reactor: https://projectreactor.io/docs/core/release/reference/#reactor-tools-debug
    Hooks.onOperatorDebug();
    ReactorDebugAgent.init();
  }
}
