package io.liquidsoftware.base.user.api.arch

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class DependencyRuleTests {

  @Test
  fun userApiMustNotDependOnLegacyWorkflowPackage() {
    noClasses()
      .that()
      .resideInAPackage("io.liquidsoftware.base.user..")
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage("io.liquidsoftware.common.workflow..")
      .check(
        ClassFileImporter()
          .importPackages("io.liquidsoftware.base.user..")
      )
  }

  @Test
  fun userApiMustNotDependOnSpring() {
    noClasses()
      .that()
      .resideInAPackage("io.liquidsoftware.base.user..")
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage("org.springframework..")
      .check(
        ClassFileImporter()
          .importPackages("io.liquidsoftware.base.user..")
      )
  }
}
