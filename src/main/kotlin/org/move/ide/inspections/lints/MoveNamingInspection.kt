package org.move.ide.inspections.lints

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import org.move.ide.inspections.MoveLocalInspectionTool
import org.move.lang.core.psi.MoveBindingPat
import org.move.lang.core.psi.MoveConstDef
import org.move.lang.core.psi.MoveStructSignature
import org.move.lang.core.psi.MoveVisitor

abstract class MoveNamingInspection(private val elementTitle: String) : MoveLocalInspectionTool() {

    override fun getDisplayName() = "$elementTitle naming convention"

    override val isSyntaxOnly: Boolean = true
}

class MoveConstNamingInspection : MoveNamingInspection("Constant") {
    override fun buildMoveVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : MoveVisitor() {
        override fun visitConstDef(o: MoveConstDef) {
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

class MoveStructNamingInspection: MoveNamingInspection("Struct") {
    override fun buildMoveVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : MoveVisitor() {
        override fun visitStructSignature(o: MoveStructSignature) {
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

class MoveLocalBindingNamingInspection: MoveNamingInspection("Local variable") {
    override fun buildMoveVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : MoveVisitor() {
        override fun visitBindingPat(o: MoveBindingPat) {
            // filter out constants
            if (o.parent is MoveConstDef) return

            val ident = o.identifier
            val name = ident.text
            if (!name.startsWithLowerCaseLetter()) {
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
