package io.liquidsoftware.base.server.config

import io.liquidsoftware.base.user.application.port.`in`.SystemFindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.SystemUserFoundEvent
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.JwtProperties
import io.liquidsoftware.common.security.JwtTokenAuthenticationFilter
import io.liquidsoftware.common.security.JwtTokenService
import io.liquidsoftware.common.security.runAsSuperUserBlocking
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import jakarta.servlet.DispatcherType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

  val log by LoggerDelegate()

  @Bean
  fun userDetailsService(dispatcher: WorkflowDispatcher): UserDetailsService = UserDetailsService { username ->
    runAsSuperUserBlocking {
      val user = dispatcher.dispatch<SystemUserFoundEvent>(SystemFindUserByEmailQuery(username))
        .fold(
          { ex ->
            log.debug("Failed to find user with username: $username", ex)
            throw UsernameNotFoundException("User not found: $username")
          },
          { it.userDetailsDto }
        )
      user
    }
  }

  @Bean
  fun securityFilterChain(
    http: HttpSecurity,
    authFilter: JwtTokenAuthenticationFilter,
    authenticationProvider: AuthenticationProvider
  ): SecurityFilterChain {
    return http
      .csrf { it.disable() }
      .httpBasic { it.disable() }
      .authorizeHttpRequests { it
        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
        .requestMatchers("/api/**").authenticated()
        .requestMatchers("/auth/login").permitAll()
        .requestMatchers("/user/register").permitAll()
        .requestMatchers("/error").permitAll() // required for proper error reporting back to client
      }
      .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
      .authenticationProvider(authenticationProvider)
      .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter::class.java)
      .build()
  }
  @Bean
  fun jwtTokenService(jwtProperties: JwtProperties) : JwtTokenService = JwtTokenService(jwtProperties)

  @Bean
  fun authenticationProvider(
    userDetailsService: UserDetailsService,
    passwordEncoder: PasswordEncoder): AuthenticationProvider {

    val authenticationProvider = DaoAuthenticationProvider()
    authenticationProvider.setUserDetailsService(userDetailsService)
    authenticationProvider.setPasswordEncoder(passwordEncoder)
    return authenticationProvider
  }

  @Bean
  fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager {
    return config.authenticationManager
  }
}
