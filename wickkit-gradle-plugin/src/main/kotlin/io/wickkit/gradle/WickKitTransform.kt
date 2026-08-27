package io.wickkit.gradle

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import org.objectweb.asm.ClassVisitor

abstract class WickKitTransform : AsmClassVisitorFactory<WickKitTransformParameters> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor,
    ): ClassVisitor = WickKitClassVisitor(nextClassVisitor)

    override fun isInstrumentable(classData: ClassData): Boolean {
        val name = classData.className
        val appPackage = parameters.get().appPackage.get()
        return name.startsWith(appPackage) && '$' !in name
    }
}
