package io.wickkit.gradle

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

private const val ASM_API = Opcodes.ASM9
private const val COMPOSER_DESC = "Landroidx/compose/runtime/Composer;"
private const val NON_RESTARTABLE_DESC = "Landroidx/compose/runtime/NonRestartableComposable;"
private const val TRACKER_OWNER = "io/wickkit/compose/WickKitComposeTracker"
private const val TRACKER_FIELD_DESC = "Lio/wickkit/compose/WickKitComposeTracker;"
private const val TRACKER_METHOD_DESC = "(Ljava/lang/String;)V"
private const val SYNTHETIC_FLAGS =
    Opcodes.ACC_ABSTRACT or Opcodes.ACC_NATIVE or Opcodes.ACC_BRIDGE or Opcodes.ACC_SYNTHETIC

internal class WickKitClassVisitor(next: ClassVisitor) : ClassVisitor(ASM_API, next) {

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<String>?,
    ): MethodVisitor? {
        val nextMethodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
        val shouldInstrument = access and SYNTHETIC_FLAGS == 0 &&
            '$' !in name && !name.startsWith('<') &&
            COMPOSER_DESC in descriptor
        return if (shouldInstrument && nextMethodVisitor != null) {
            WickKitMethodVisitor(next = nextMethodVisitor, composableName = name)
        } else {
            nextMethodVisitor
        }
    }
}

private class WickKitMethodVisitor(
    next: MethodVisitor,
    private val composableName: String,
) : MethodVisitor(ASM_API, next) {

    private var nonRestartable = false

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
        if (descriptor == NON_RESTARTABLE_DESC) nonRestartable = true
        return super.visitAnnotation(descriptor, visible)
    }

    override fun visitCode() {
        super.visitCode()
        if (!nonRestartable) {
            mv.visitFieldInsn(
                Opcodes.GETSTATIC,
                TRACKER_OWNER,
                "INSTANCE",
                TRACKER_FIELD_DESC,
            )
            mv.visitLdcInsn(composableName)
            mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                TRACKER_OWNER,
                "onRecompose",
                TRACKER_METHOD_DESC,
                false,
            )
        }
    }
}
