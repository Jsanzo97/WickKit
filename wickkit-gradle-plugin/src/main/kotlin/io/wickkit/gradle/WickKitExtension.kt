package io.wickkit.gradle

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class WickKitExtension {
    abstract val enabled: Property<Boolean>
    abstract val packagePrefixes: ListProperty<String>

    init {
        enabled.convention(true)
        packagePrefixes.convention(emptyList())
    }
}
