package ventures.dvx.common.axon.security

import org.junit.jupiter.api.Test
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import ventures.dvx.common.logging.LoggerDelegate

internal class ExecutionContextTest {

  val log by LoggerDelegate()

  @Test
  fun runAsSuperUser() {
    val monoAsSuperUser = Mono.just("Hello World")
      .flatMap {
        // check running as running as super user
        ReactiveSecurityContextHolder.getContext()
          .map { (it.authentication.principal as UserDetails).username }
      }
      .runAsSuperUser()

    StepVerifier.create(monoAsSuperUser.log())
      .expectNext("SYSTEM")
      .verifyComplete()


    val monoNoSuperuser = Mono.just("Hello World")
      .flatMap {
        // check running as running as super user
        ReactiveSecurityContextHolder.getContext()
          .map { (it.authentication.principal as UserDetails) }
          .switchIfEmpty(Mono.error(RuntimeException("NO LOGGED IN USER")))
      }

    StepVerifier.create(monoNoSuperuser)
      .expectError(RuntimeException::class.java)
      .verify()
  }
}
