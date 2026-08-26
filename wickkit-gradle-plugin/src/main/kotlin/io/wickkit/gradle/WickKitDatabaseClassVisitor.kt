package io.wickkit.gradle

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

private const val ASM_API = Opcodes.ASM9
private const val SQLITE_DATABASE = "android/database/sqlite/SQLiteDatabase"
private const val SQLITE_DATABASE_DESC = "L$SQLITE_DATABASE;"
private const val REGISTRY_OWNER = "io/wickkit/database/WickKitDatabaseRegistry"

private const val SQLDELIGHT_DRIVER_V1 = "com/squareup/sqldelight/android/AndroidSqliteDriver"
private const val SQLDELIGHT_DRIVER_V2 = "app/cash/sqldelight/driver/android/AndroidSqliteDriver"
private const val SQLDELIGHT_REGISTRY_OWNER = "io/wickkit/database/WickKitSqlDelightRegistry"

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

    private var pendingSqlDelightNew = false

    override fun visitTypeInsn(opcode: Int, type: String) {
        super.visitTypeInsn(opcode, type)
        if (opcode == Opcodes.NEW && (type == SQLDELIGHT_DRIVER_V1 || type == SQLDELIGHT_DRIVER_V2)) {
            pendingSqlDelightNew = true
        }
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean,
    ) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)

        if (descriptor.endsWith(")$SQLITE_DATABASE_DESC")) {
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

        val isSqlDelightInit = pendingSqlDelightNew &&
            opcode == Opcodes.INVOKESPECIAL &&
            name == "<init>" &&
            (owner == SQLDELIGHT_DRIVER_V1 || owner == SQLDELIGHT_DRIVER_V2)

        if (isSqlDelightInit) {
            pendingSqlDelightNew = false
            mv.visitInsn(Opcodes.DUP)
            mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                SQLDELIGHT_REGISTRY_OWNER,
                "register",
                "(Ljava/lang/Object;)V",
                false,
            )
        }
    }
}
