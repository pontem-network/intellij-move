package org.move.lang.core.psi.mixins

import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.util.PlatformIcons
import org.move.ide.MvIcons
import org.move.ide.annotator.BUILTIN_FUNCTIONS
import org.move.lang.core.psi.MvFunctionDef
import org.move.lang.core.psi.MvFunctionSignature
import org.move.lang.core.psi.MvNativeFunctionDef
import org.move.lang.core.psi.ext.inferTypeTy
import org.move.lang.core.psi.ext.parametersText
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnit
import org.move.lang.core.types.ty.TyUnknown
import javax.swing.Icon

val MvFunctionSignature.functionDef get() = this.parent as? MvFunctionDef

val MvFunctionSignature.isNative get() = this.parent is MvNativeFunctionDef

val MvFunctionSignature.isBuiltinFunc get() = this.isNative && this.name in BUILTIN_FUNCTIONS

val MvFunctionSignature.resolvedReturnType: Ty
    get() {
        val returnTypeElement = this.returnType
        return if (returnTypeElement == null) {
            TyUnit
        } else {
            returnTypeElement.type?.inferTypeTy() ?: TyUnknown
        }
    }

abstract class MvFunctionSignatureMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                           MvFunctionSignature {
    var builtIn = false

    override fun canNavigate(): Boolean = !builtIn
    override fun canNavigateToSource(): Boolean = !builtIn

    override fun getIcon(flags: Int): Icon = MvIcons.FUNCTION

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
