package ventures.dvx

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan("ventures.dvx")
class DvxApplication

fun main(args: Array<String>) {
  runApplication<DvxApplication>(*args)
}

