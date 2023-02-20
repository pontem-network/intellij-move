/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.lang.core.psi

import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.ext.stubParent
import kotlin.reflect.KClass

/**
 * A PSI element that holds modification tracker for some reason.
 * This is mostly used to invalidate cached type inference results.
 */
interface MvModificationTrackerOwner : MvElement {
    val modificationTracker: ModificationTracker

    /**
     * Increments local modification counter if needed.
     *
     * If and only if false returned,
     * [MvPsiManager.moveStructureModificationTracker]
     * will be incremented.
     *
     * @param element the changed psi element
     * @see org.rust.lang.core.psi.RsPsiManagerImpl.updateModificationCount
     */
    fun incModificationCount(element: PsiElement): Boolean
}

fun PsiElement.findModificationTrackerOwner(strict: Boolean): MvModificationTrackerOwner? {
    return findContextOfType(strict, MvNamedElement::class)
            as? MvModificationTrackerOwner
}

// We have to process contexts without index access because accessing indices during PSI event processing is slow.
//private val PsiElement.contextWithoutIndexAccess: PsiElement?
//    //    get() = if (this is RsExpandedElement) {
////        RsExpandedElement.getContextImpl(this, isIndexAccessForbidden = true)
////    } else {
//    get() = stubParent
////    }

@Suppress("UNCHECKED_CAST")
private fun <T : PsiElement> PsiElement.findContextOfType(
    strict: Boolean,
    vararg classes: KClass<out T>
): T? {
    var element = if (strict) stubParent else this

    while (element != null && !classes.any { it.isInstance(element) }) {
        element = element.stubParent
    }

    return element as T?
}
