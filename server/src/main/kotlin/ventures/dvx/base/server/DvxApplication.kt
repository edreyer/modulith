package ventures.dvx.base.server

import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import ventures.dvx.base.server.config.ServerConfig
import ventures.dvx.base.user.config.UserModuleConfig

@SpringBootApplication(scanBasePackages = ["ventures.dvx.base.server"])
class DvxApplication

fun main(args: Array<String>) {
  val parent = SpringApplicationBuilder()
    .parent(DvxApplication::class.java, ServerConfig::class.java)
    .web(WebApplicationType.REACTIVE)

  parent.run(*args)

  parent
    .child(UserModuleConfig::class.java)
    .properties("spring.config.name=user")
    .web(WebApplicationType.NONE)
    .run(*args)
}

