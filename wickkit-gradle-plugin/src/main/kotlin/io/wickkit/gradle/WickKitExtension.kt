package io.wickkit.gradle

import org.gradle.api.provider.Property

abstract class WickKitExtension {
    abstract val enabled: Property<Boolean>

    init {
        enabled.convention(true)
    }
}
