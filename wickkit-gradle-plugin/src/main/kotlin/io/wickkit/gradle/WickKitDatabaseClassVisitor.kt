package io.wickkit.gradle

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

private const val ASM_API = Opcodes.ASM9
private const val SQLITE_DATABASE = "android/database/sqlite/SQLiteDatabase"
private const val SQLITE_DATABASE_DESC = "L$SQLITE_DATABASE;"
private const val REGISTRY_OWNER = "io/wickkit/database/WickKitDatabaseRegistry"

internal class WickKitDatabaseClassVisitor(next: ClassVisitor) : ClassVisitor(ASM_API, next) {

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<String>?,
    ): MethodVisitor? {
        val next = super.visitMethod(access, name, descriptor, signature, exceptions)
        return if (next != null) WickKitDatabaseMethodVisitor(next) else null
    }
}

private class WickKitDatabaseMethodVisitor(next: MethodVisitor) : MethodVisitor(ASM_API, next) {

    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean,
    ) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)

        val returnsDatabase = descriptor.endsWith(")$SQLITE_DATABASE_DESC")
        if (!returnsDatabase) return

        val isOpenDatabase = opcode == Opcodes.INVOKESTATIC &&
            owner == SQLITE_DATABASE &&
            name == "openDatabase"

        val isGetDatabase = (name == "getWritableDatabase" || name == "getReadableDatabase") &&
            descriptor == "()$SQLITE_DATABASE_DESC"

        if (isOpenDatabase || isGetDatabase) {
            mv.visitInsn(Opcodes.DUP)
            mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                REGISTRY_OWNER,
                "register",
                "($SQLITE_DATABASE_DESC)V",
                false,
            )
        }
    }
}
