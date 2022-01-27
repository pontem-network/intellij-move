package org.move.ide.inspections.lints

import com.intellij.codeInspection.ProblemsHolder
import org.move.ide.inspections.MvLocalInspectionTool
import org.move.lang.core.psi.MvBindingPat
import org.move.lang.core.psi.MvConstDef
import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.MvVisitor

abstract class MvNamingInspection(private val elementTitle: String) : MvLocalInspectionTool() {

    override fun getDisplayName() = "$elementTitle naming convention"

    override val isSyntaxOnly: Boolean = true
}

class MvConstNamingInspection : MvNamingInspection("Constant") {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : MvVisitor() {
        override fun visitConstDef(o: MvConstDef) {
            val ident = o.bindingPat?.identifier ?: return
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

class MvStructNamingInspection: MvNamingInspection("Struct") {
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

class MvLocalBindingNamingInspection: MvNamingInspection("Local variable") {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : MvVisitor() {
        override fun visitBindingPat(o: MvBindingPat) {
            // filter out constants
            if (o.parent is MvConstDef) return

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
