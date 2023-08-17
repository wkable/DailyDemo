import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.Dependency
import org.gradle.api.initialization.dsl.ScriptHandler

fun String.isStableVersion(): Boolean {
    val stableKeyword =
        listOf("RELEASE", "FINAL", "GA").any { uppercase().contains(it) }
    return stableKeyword || Regex("^[0-9,.v-]+(-r)?$").matches(this)
}

fun DependencyHandler.classpath(dependencyNotation: Any): Dependency? =
    add(ScriptHandler.CLASSPATH_CONFIGURATION, dependencyNotation)
