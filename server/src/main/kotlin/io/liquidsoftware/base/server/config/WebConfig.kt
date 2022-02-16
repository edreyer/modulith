package io.liquidsoftware.base.server.config

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
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import io.liquidsoftware.base.user.application.port.`in`.SystemFindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.SystemUserFoundEvent
import io.liquidsoftware.common.security.JwtProperties
import io.liquidsoftware.common.security.JwtTokenAuthenticationFilter
import io.liquidsoftware.common.security.JwtTokenProvider
import io.liquidsoftware.common.security.runAsSuperUser
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import io.liquidsoftware.common.workflow.WorkflowDispatcher.log

@Configuration
@EnableWebFluxSecurity
class WebConfig {

  @Bean
  fun corsWebFilter() : CorsWebFilter {
    val corsConfiguration = CorsConfiguration().apply {
        this.allowedOrigins = listOf("localhost")
      }
    val source: CorsConfigurationSource = UrlBasedCorsConfigurationSource().apply {
      this.registerCorsConfiguration("/**", corsConfiguration)
    }
    return CorsWebFilter(source)
  }

  @Bean
  fun jwtTokenProvider(jwtProperties: JwtProperties) : JwtTokenProvider =
    JwtTokenProvider(jwtProperties)

  @Bean
  fun reactiveAuthenticationManager(
    userDetailsService: ReactiveUserDetailsService,
    passwordEncoder: PasswordEncoder
  ): ReactiveAuthenticationManager {
    val authenticationManager = UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService)
    authenticationManager.setPasswordEncoder(passwordEncoder)
    return authenticationManager
  }

  @Bean
  fun userDetailsService(): ReactiveUserDetailsService {
    return ReactiveUserDetailsService { username ->
      mono {
        WorkflowDispatcher.dispatch<SystemUserFoundEvent>(SystemFindUserByEmailQuery(username))
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



}
