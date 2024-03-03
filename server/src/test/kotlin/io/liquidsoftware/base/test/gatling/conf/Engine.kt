package io.liquidsoftware.base.test.gatling.conf

import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

object Engine {

  @JvmStatic
  fun main(args: Array<String>) {
    val props = GatlingPropertiesBuilder()
      .resourcesDirectory(IDEPathHelper.mavenResourcesDirectory.toString())
      .resultsDirectory(IDEPathHelper.resultsDirectory.toString())
      .binariesDirectory(IDEPathHelper.mavenBinariesDirectory.toString())

    Gatling.fromMap(props.build())
  }
}
