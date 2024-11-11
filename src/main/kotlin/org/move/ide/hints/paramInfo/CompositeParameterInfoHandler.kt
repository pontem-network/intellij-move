package org.move.ide.hints.paramInfo

import com.intellij.lang.parameterInfo.*
import com.intellij.psi.PsiElement
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.ext.ancestors
import org.move.lang.core.psi.ext.elementType
import org.move.lang.core.psi.ext.startOffset

class CompositeParameterInfoHandler: ParameterInfoHandler<PsiElement, ParameterInfoProvider.ParametersInfo> {

    val providers: List<ParameterInfoProvider> =
        buildList {
            add(FunctionParameterInfoProvider())
            add(TypeParameterInfoProvider())
        }

    // should store the information about arguments into the `itemsToShow` variable to use later
    override fun findElementForParameterInfo(context: CreateParameterInfoContext): PsiElement? {
        val (listElement, provider) = findTargetElement(context) ?: return null
        context.itemsToShow =
            provider.findParameterInfo(listElement)?.let { arrayOf(it) } ?: return null
        return listElement
    }

    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): PsiElement? {
        val (listElement, _) = findTargetElement(context) ?: return null
        return listElement
    }

    override fun showParameterInfo(element: PsiElement, context: CreateParameterInfoContext) {
        context.showHint(element, element.startOffset, this)
    }

    override fun updateParameterInfo(listElement: PsiElement, context: UpdateParameterInfoContext) {
        if (context.parameterOwner != listElement) {
            context.removeHint()
            return
        }
        val contextOffset = context.offset
        val currentParameterIndex =
            if (listElement.startOffset == contextOffset) {
                -1
            } else {
                ParameterInfoUtils.getCurrentParameterIndex(
                    listElement.node,
                    context.offset,
                    MvElementTypes.COMMA
                )
            }
        context.setCurrentParameter(currentParameterIndex)
    }

    override fun updateUI(paramsInfo: ParameterInfoProvider.ParametersInfo, context: ParameterInfoUIContext) {
        val argumentRange = paramsInfo.getRangeInParent(context.currentParameterIndex)
        context.setupUIComponentPresentation(
            paramsInfo.presentText,
            argumentRange.startOffset,
            argumentRange.endOffset,
            !context.isUIComponentEnabled,
            false,
            false,
            context.defaultParameterColor
        )
    }

    private fun findTargetElement(context: ParameterInfoContext): Pair<PsiElement, ParameterInfoProvider>? {
        val element = context.file.findElementAt(context.offset) ?: return null
        return run {
            for (ancestor in element.ancestors) {
                for (provider in providers) {
                    if (provider.targetElementType == ancestor.elementType) {
                        return@run ancestor to provider
                    }
                }
            }
            null
        }
    }
}