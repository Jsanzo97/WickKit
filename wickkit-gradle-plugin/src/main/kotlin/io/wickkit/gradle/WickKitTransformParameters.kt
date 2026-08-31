package io.wickkit.gradle

import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

interface WickKitTransformParameters : InstrumentationParameters {
    @get:Input
    val appPackage: Property<String>

    @get:Input
    val packagePrefixes: ListProperty<String>
}
