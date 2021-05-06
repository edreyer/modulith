package ventures.dvx.arch

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.conditions.ArchConditions
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition


abstract class ArchitectureElement(val basePackage: String) {
  fun fullQualifiedPackage(relativePackage: String): String {
    return "$basePackage.$relativePackage"
  }

  fun denyEmptyPackage(packageName: String) {
    ArchRuleDefinition.classes()
      .that()
      .resideInAPackage(matchAllClassesInPackage(packageName))
      .should(ArchConditions.containNumberOfElements(DescribedPredicate.greaterThanOrEqualTo(1)))
      .check(classesInPackage(packageName))
  }

  private fun classesInPackage(packageName: String): JavaClasses {
    return ClassFileImporter().importPackages(packageName)
  }

  fun denyEmptyPackages(packages: List<String>) {
    for (packageName in packages) {
      denyEmptyPackage(packageName)
    }
  }

  companion object {
    fun denyDependency(fromPackageName: String, toPackageName: String, classes: JavaClasses) {
      ArchRuleDefinition.noClasses()
        .that()
        .resideInAPackage("$fromPackageName..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("$toPackageName..")
        .check(classes)
    }

    fun denyAnyDependency(fromPackages: List<String>, toPackages: List<String>, classes: JavaClasses) {
      for (fromPackage in fromPackages) {
        for (toPackage in toPackages) {
          ArchRuleDefinition.noClasses()
            .that()
            .resideInAPackage(matchAllClassesInPackage(fromPackage))
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(matchAllClassesInPackage(toPackage))
            .check(classes)
        }
      }
    }

    fun matchAllClassesInPackage(packageName: String): String {
      return "$packageName.."
    }
  }
}
