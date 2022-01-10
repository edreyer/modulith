package io.liquidsoftware.base

import io.liquidsoftware.base.server.MicrolithApplication
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
  classes = [MicrolithApplication::class],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ApplicationTests {

  @Test
  fun contextLoads() {}

}
