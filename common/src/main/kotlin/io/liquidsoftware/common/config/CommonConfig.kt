package io.liquidsoftware.common.config

import io.liquidsoftware.common.security.spring.AuthenticationAccessSubjectResolver
import io.liquidsoftware.common.security.spring.SpringSecurityAccessSubjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder


@Configuration
@EnableConfigurationProperties
@ComponentScan(basePackages = ["io.liquidsoftware.common"])
@ConfigurationPropertiesScan(basePackages = ["io.liquidsoftware.common"])
class CommonConfig {

  @Bean
  fun passwordEncoder(): PasswordEncoder =
    PasswordEncoderFactories.createDelegatingPasswordEncoder()

  @Bean
  @ConditionalOnMissingBean(SpringSecurityAccessSubjectProvider::class)
  fun springSecurityAccessSubjectProvider(
    resolver: AuthenticationAccessSubjectResolver,
  ): SpringSecurityAccessSubjectProvider =
    SpringSecurityAccessSubjectProvider(resolver)

}
