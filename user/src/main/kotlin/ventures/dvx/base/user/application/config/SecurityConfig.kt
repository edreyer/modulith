package ventures.dvx.base.user.application.config

import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import ventures.dvx.base.user.application.port.`in`.SystemFindUserByEmailQuery
import ventures.dvx.base.user.application.port.`in`.SystemFindUserByEmailWorkflow
import ventures.dvx.base.user.application.port.`in`.SystemFindUserEvent
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dvx.common.security.JwtProperties
import ventures.dvx.common.security.JwtTokenAuthenticationFilter
import ventures.dvx.common.security.JwtTokenProvider
import ventures.dvx.common.security.runAsSuperUser
import ventures.dvx.common.workflow.WorkflowDispatcher

/**
 * JWT for WebFlux from:
 * https://medium.com/zero-equals-false/protect-rest-apis-with-spring-security-reactive-and-jwt-7b209a0510f1
 */
@Configuration
@EnableWebFluxSecurity
internal class SecurityConfig {

  val log by LoggerDelegate()

  @Bean
  fun jwtTokenProvider(jwtProperties: JwtProperties) : JwtTokenProvider =
    JwtTokenProvider(jwtProperties)

  @Bean
  fun springWebFilterChain(
    http: ServerHttpSecurity,
    tokenProvider: JwtTokenProvider,
    reactiveAuthenticationManager: ReactiveAuthenticationManager
  ): SecurityWebFilterChain {
    return http
      .csrf { it.disable() }
      .httpBasic { it.disable() }
      .authenticationManager(reactiveAuthenticationManager)
      .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
      .authorizeExchange { it
        .pathMatchers("/api/**").authenticated()
        .pathMatchers("/auth/login").permitAll()
        .pathMatchers("/user/register").permitAll()
        .anyExchange().permitAll()
      }
      .addFilterAt(JwtTokenAuthenticationFilter(tokenProvider), SecurityWebFiltersOrder.HTTP_BASIC)
      .build()
  }

  @Bean
  fun userDetailsService(findUser: SystemFindUserByEmailWorkflow): ReactiveUserDetailsService {
    return ReactiveUserDetailsService { username ->
      mono {
        WorkflowDispatcher.dispatch<SystemFindUserEvent>(SystemFindUserByEmailQuery(username))
          .fold(
            { it.userDetailsDto },
            { ex ->
              log.debug("Failed to find user with username: $username", ex)
              null}
          )
      }
        .runAsSuperUser()
        .map { it }
    }
  }

  @Bean
  fun reactiveAuthenticationManager(
    userDetailsService: ReactiveUserDetailsService,
    passwordEncoder: PasswordEncoder
  ): ReactiveAuthenticationManager {
    val authenticationManager = UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService)
    authenticationManager.setPasswordEncoder(passwordEncoder)
    return authenticationManager
  }

}
