package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MoveModuleDef
import org.move.lang.core.psi.MoveQualPathReferenceElement

abstract class MoveCompletionProvider : CompletionProvider<CompletionParameters>() {
    abstract val elementPattern: ElementPattern<out PsiElement>

//    protected fun resolveModule(refElement: MoveQualPathReferenceElement): MoveModuleDef? {
//        val moduleRef = refElement.qualPath.moduleRef
//        if (moduleRef != null) {
//            return moduleRef.reference.resolve() as? MoveModuleDef
//        }
//        return null
//    }
}