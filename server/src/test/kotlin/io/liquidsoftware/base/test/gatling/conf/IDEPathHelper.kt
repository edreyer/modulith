package io.liquidsoftware.base.test.gatling.conf

import java.nio.file.Path
import java.nio.file.Paths
import java.util.Objects.requireNonNull

object IDEPathHelper {
  private val projectRootDir = Paths.get(requireNonNull(javaClass.getResource("gatling.conf"), "Couldn't locate gatling.conf").toURI()).parent.parent.parent
  private val mavenTargetDirectory: Path = projectRootDir.resolve("target")
  private val mavenSrcTestDirectory: Path = projectRootDir.resolve("src").resolve("test")

  val mavenSourcesDirectory: Path = mavenSrcTestDirectory.resolve("kotlin")
  val mavenResourcesDirectory: Path = mavenSrcTestDirectory.resolve("resources")
  val mavenBinariesDirectory: Path = mavenTargetDirectory.resolve("test-classes")
  val resultsDirectory: Path = mavenTargetDirectory.resolve("gatling")
//  val recorderConfigFile: Path = mavenResourcesDirectory.resolve("recorder.conf")
}
