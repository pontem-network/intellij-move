package org.move.ide.inspections

import com.intellij.codeInspection.ProblemsHolder
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.contains
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.types.infer.inference

abstract class MvNamingInspection(private val elementTitle: String) : MvLocalInspectionTool() {

    override fun getDisplayName() = "$elementTitle naming convention"

    override val isSyntaxOnly: Boolean = true
}

class MvConstNamingInspection : MvNamingInspection("Constant") {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : MvVisitor() {
        override fun visitConst(o: MvConst) {
            val ident = o.identifier ?: return
            val name = ident.text
            if (!name.startsWithUpperCaseLetter()) {
                holder.registerProblem(
                    ident,
                    "Invalid constant name `$name`. Constant names must start with 'A'..'Z'"
                )
            }
        }
    }
}

class MvFunctionNamingInspection : MvNamingInspection("Function") {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : MvVisitor() {

        override fun visitFunction(o: MvFunction) = checkFunctionName(o)

        override fun visitSpecFunction(o: MvSpecFunction) = checkFunctionName(o)

        private fun checkFunctionName(o: MvFunctionLike) {
            val ident = o.nameIdentifier ?: return
            val name = ident.text
            if (name.startsWithUnderscore()) {
                holder.registerProblem(
                    ident,
                    "Invalid function name '$name'. Function names cannot start with '_'"
                )
            }
        }
    }
}

class MvStructNamingInspection : MvNamingInspection("Struct") {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : MvVisitor() {
        override fun visitStruct(o: MvStruct) {
            val ident = o.identifier ?: return
            val name = ident.text
            if (!name.startsWithUpperCaseLetter()) {
                holder.registerProblem(
                    ident,
                    "Invalid struct name `$name`. Struct names must start with 'A'..'Z'"
                )
            }
        }
    }
}

class MvLocalBindingNamingInspection : MvNamingInspection("Local variable") {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : MvVisitor() {
        override fun visitPatBinding(o: MvPatBinding) {
            val parent = o.parent
            // filter out constants
            if (parent is MvConst) return

            // violations allowed in msl
            if (o.isMsl()) return

            val matchArm = o.ancestorStrict<MvMatchArm>()
            if (matchArm != null && matchArm.pat.contains(o)) {
                // match arm lhs
                return
            }

            val ident = o.identifier
            val name = ident.text
            val trimmed = name.trimStart('_')
            if (trimmed.isNotBlank() && !trimmed.startsWithLowerCaseLetter()) {
                holder.registerProblem(
                    ident,
                    "Invalid local variable name `$name`. Local variable names must start with 'a'..'z'"
                )
            }
        }
    }
}

fun String.startsWithUpperCaseLetter(): Boolean = this[0].isUpperCase()

fun String.startsWithLowerCaseLetter(): Boolean = this[0].isLowerCase()

fun String.startsWithUnderscore(): Boolean = this[0] == '_'
