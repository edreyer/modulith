package ventures.dvx.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.factory.PasswordEncoderFactories

import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class AppConfig {

  @Bean
  fun passwordEncoder(): PasswordEncoder =
    PasswordEncoderFactories.createDelegatingPasswordEncoder()

}
