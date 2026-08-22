package io.wickkit.gradle

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class WickKitPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.withId("com.android.application") {
            val android = target.extensions.getByType(AndroidComponentsExtension::class.java)
            android.onVariants { variant ->
                variant.instrumentation.transformClassesWith(
                    WickKitTransform::class.java,
                    InstrumentationScope.PROJECT,
                ) {}
                variant.instrumentation.setAsmFramesComputationMode(
                    FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS,
                )
            }
        }
    }
}
