package io.wickkit.gradle

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.objectweb.asm.ClassVisitor

abstract class WickKitDatabaseTransform : AsmClassVisitorFactory<InstrumentationParameters.None> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor,
    ): ClassVisitor = WickKitDatabaseClassVisitor(nextClassVisitor)

    override fun isInstrumentable(classData: ClassData): Boolean {
        val name = classData.className
        return !name.startsWith("io.wickkit") &&
            !name.startsWith("android.") &&
            !name.startsWith("java.") &&
            !name.startsWith("kotlin.") &&
            !name.startsWith("kotlinx.")
    }
}
