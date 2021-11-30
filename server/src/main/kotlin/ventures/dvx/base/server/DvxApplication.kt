package ventures.dvx.base.server

import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import ventures.dvx.base.server.config.ServerConfig
import ventures.dvx.base.user.config.UserModuleConfig

@SpringBootApplication(scanBasePackages = ["ventures.dvx.base.server"])
class DvxApplication

fun main(args: Array<String>) {
  SpringApplicationBuilder()
    .parent(DvxApplication::class.java, ServerConfig::class.java).web(WebApplicationType.REACTIVE)
      .child(UserModuleConfig::class.java).web(WebApplicationType.REACTIVE)
    .run(*args);
}

