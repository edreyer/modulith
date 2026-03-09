package io.liquidsoftware.base.server.arch

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.liquidsoftware.base.server.ModulithApplication
import io.liquidsoftware.base.server.config.ServerConfig
import org.junit.jupiter.api.Test

class DependencyRuleTests {

  @Test
  fun onlyCompositionRootMayDependOnBoundedContextImplementations() {
    noClasses()
      .that()
      .resideInAPackage("io.liquidsoftware.base.server..")
      .and()
      .resideOutsideOfPackages("io.liquidsoftware.base.server.config..")
      .and()
      .doNotHaveFullyQualifiedName(ModulithApplication::class.java.name)
      .and()
      .doNotHaveFullyQualifiedName("io.liquidsoftware.base.server.ModulithApplicationKt")
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage(
        "io.liquidsoftware.base.user.adapter..",
        "io.liquidsoftware.base.user.config..",
        "io.liquidsoftware.base.booking.adapter..",
        "io.liquidsoftware.base.booking.config..",
        "io.liquidsoftware.base.payment.adapter..",
        "io.liquidsoftware.base.payment.config.."
      )
      .check(
        ClassFileImporter()
          .importPackages("io.liquidsoftware.base.server..")
      )
  }

  @Test
  fun onlyServerConfigMayImportLocalModuleAdapterConfigurations() {
    noClasses()
      .that()
      .resideInAPackage("io.liquidsoftware.base.server.config..")
      .and()
      .doNotHaveFullyQualifiedName(ServerConfig::class.java.name)
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage(
        "io.liquidsoftware.base.user.adapter..",
        "io.liquidsoftware.base.user.config..",
        "io.liquidsoftware.base.booking.adapter..",
        "io.liquidsoftware.base.booking.config..",
        "io.liquidsoftware.base.payment.adapter..",
        "io.liquidsoftware.base.payment.config.."
      )
      .check(
        ClassFileImporter()
          .importPackages("io.liquidsoftware.base.server..")
      )
  }
}
