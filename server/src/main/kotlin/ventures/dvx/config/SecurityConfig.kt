package ventures.dvx.config

import org.axonframework.queryhandling.QueryGateway
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authorization.AuthorizationContext
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import reactor.core.publisher.Mono
import ventures.dvx.base.user.api.FindUserQuery
import ventures.dvx.common.security.JwtProperties
import ventures.dvx.common.security.JwtTokenAuthenticationFilter
import ventures.dvx.common.security.JwtTokenProvider


/**
 * JWT for WebFlux from:
 * https://medium.com/zero-equals-false/protect-rest-apis-with-spring-security-reactive-and-jwt-7b209a0510f1
 */
@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

  @Bean
  fun passwordEncoder(): PasswordEncoder {
    return BCryptPasswordEncoder()
  }

  @Bean
  fun jwtTokenProvider(jwtProperties: JwtProperties) : JwtTokenProvider =
    JwtTokenProvider(jwtProperties)

  @Bean
  fun springWebFilterChain(
    http: ServerHttpSecurity,
    tokenProvider: JwtTokenProvider,
    reactiveAuthenticationManager: ReactiveAuthenticationManager,
  ): SecurityWebFilterChain {
    return http
      .csrf { it.disable() }
      .httpBasic { it.disable() }
      .authenticationManager(reactiveAuthenticationManager)
      .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
      .authorizeExchange { it
//        .pathMatchers("/api/**").access(this::currentUserMatchesPath)
        .pathMatchers("/user/login").permitAll()
        .pathMatchers("/user/confirmToken").permitAll()
        .pathMatchers("/api/**").authenticated()
        .anyExchange().permitAll()
      }
      .addFilterAt(JwtTokenAuthenticationFilter(tokenProvider), SecurityWebFiltersOrder.HTTP_BASIC)
      .build()
  }

  private fun currentUserMatchesPath(
    authentication: Mono<Authentication>,
    context: AuthorizationContext,
  ): Mono<AuthorizationDecision> {
    return authentication
      .map{ context.variables["user"] == it.name }
      .map { AuthorizationDecision(it) }
  }

  @Bean
  fun userDetailsService(queryGateway: QueryGateway): ReactiveUserDetailsService {
    return ReactiveUserDetailsService { username ->
      queryGateway.query(FindUserQuery(username), ventures.dvx.base.user.api.User::class.java)
        ?.thenApply { User(it.username, "", it.roles.map { role -> SimpleGrantedAuthority(role) }) }
        ?.let { Mono.fromFuture(it)}
    }
  }

  @Bean
  fun reactiveAuthenticationManager(
    userDetailsService: ReactiveUserDetailsService,
    passwordEncoder: PasswordEncoder,
  ): ReactiveAuthenticationManager {
    val authenticationManager = UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService)
    authenticationManager.setPasswordEncoder(passwordEncoder)
    return authenticationManager
  }

}
