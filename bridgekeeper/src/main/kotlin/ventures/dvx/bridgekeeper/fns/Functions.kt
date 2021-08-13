package ventures.dvx.bridgekeeper.fns

/**
 * Helper function to get fully qualified class name from some type T
 */
inline fun <reified T> className(): String = T::class.qualifiedName ?: T::class.java.name
