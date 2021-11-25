package org.move.lang.core.psi.mixins

import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.util.PlatformIcons
import org.move.ide.MoveIcons
import org.move.ide.annotator.BUILTIN_FUNCTIONS
import org.move.lang.core.psi.MoveFunctionDef
import org.move.lang.core.psi.MoveFunctionSignature
import org.move.lang.core.psi.MoveNativeFunctionDef
import org.move.lang.core.psi.ext.parametersText
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnit
import org.move.lang.core.types.ty.TyUnknown
import javax.swing.Icon

val MoveFunctionSignature.functionDef get() = this.parent as? MoveFunctionDef

val MoveFunctionSignature.isNative get() = this.parent is MoveNativeFunctionDef

val MoveFunctionSignature.isBuiltinFunc get() = this.isNative && this.name in BUILTIN_FUNCTIONS

val MoveFunctionSignature.resolvedReturnType: Ty
    get() {
        val returnTypeElement = this.returnType
        return if (returnTypeElement == null) {
            TyUnit
        } else {
            returnTypeElement.type?.resolvedType() ?: TyUnknown
        }
    }

abstract class MoveFunctionSignatureMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                           MoveFunctionSignature {
    var builtIn = false

    override fun canNavigate(): Boolean = !builtIn
    override fun canNavigateToSource(): Boolean = !builtIn

    override fun getIcon(flags: Int): Icon = MoveIcons.FUNCTION

    override fun getPresentation(): ItemPresentation? {
        val name = this.name ?: return null
        val params = this.functionParameterList?.parametersText ?: "()"
        val returnTypeText = this.returnType?.type?.text ?: ""
        val tail = if (returnTypeText == "") "" else ": $returnTypeText"
        return PresentationData(
            "$name$params$tail",
            null,
            PlatformIcons.PUBLIC_ICON,
            TextAttributesKey.createTextAttributesKey("public")
        );
    }
}
