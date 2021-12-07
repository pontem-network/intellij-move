package org.move.lang.core.psi

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveVisitor

//open class MvRecursiveVisitor : MvVisitor(),
//                                  PsiRecursiveVisitor {
////    override fun visitElement(element: PsiElement) {
////        ProgressManager.checkCanceled()
////        element.acceptChildren(this)
////    }
//
////    override fun visitElement(element: MvElement) {
////        visitElement(element as PsiElement)
////    }
//}
