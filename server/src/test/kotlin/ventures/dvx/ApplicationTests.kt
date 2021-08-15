package ventures.dvx

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
  classes = [DvxApplication::class],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ApplicationTests {

  @Test
  fun contextLoads() {}

}
