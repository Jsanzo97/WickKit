package io.wickkit.gradle

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class WickKitPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        if (target.extensions.findByType(WickKitExtension::class.java) != null) {
            return
        }
        val extension = target.extensions.create("wickKit", WickKitExtension::class.java)

        if (target == target.rootProject) {
            target.subprojects { subproject ->
                subproject.plugins.apply(WickKitPlugin::class.java)
            }
        }

        target.plugins.withId("com.android.application") {
            registerTransforms(target = target, extension = extension, isApp = true)
        }

        target.plugins.withId("com.android.library") {
            registerTransforms(target = target, extension = extension, isApp = false)
        }
    }

    private fun registerTransforms(
        target: Project,
        extension: WickKitExtension,
        isApp: Boolean,
    ) {
        val androidComponents = target.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants(androidComponents.selector().withBuildType("debug")) { variant ->
            if (!extension.enabled.get()) {
                return@onVariants
            }
            variant.instrumentation.transformClassesWith(
                WickKitTransform::class.java,
                InstrumentationScope.PROJECT,
            ) {}
            if (isApp) {
                variant.instrumentation.transformClassesWith(
                    WickKitDatabaseTransform::class.java,
                    InstrumentationScope.ALL,
                ) {}
            }
            variant.instrumentation.setAsmFramesComputationMode(
                FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS,
            )
        }
    }
}
