# Milestone 16 JVM target CI fix

Kotlin 2.3 no longer accepts the deprecated `android.kotlinOptions { jvmTarget = "17" }` DSL.

The app now uses the typed compiler options DSL:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
```

Java source/target compatibility remains Java 17.

Version: 0.16.2 (29).
