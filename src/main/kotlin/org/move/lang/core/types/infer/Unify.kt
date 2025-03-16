/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.lang.core.types.infer

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import io.ktor.http.*
import org.move.ide.formatter.impl.PsiLocation
import org.move.ide.formatter.impl.elementLocation
import org.move.lang.core.psi.MvTypeParameter
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyInfer
import org.move.lang.core.types.ty.VarOrValue
import org.move.lang.core.types.ty.VarValue
import org.move.lang.toNioPathOrNull

/**
 * [UnificationTable] is map from [TyVar] to [Ty] with additional ability
 * to redirect certain K's to a single V en-masse with the help of
 * disjoint set union.
 *
 * We implement Tarjan's union-find
 * algorithm: when two keys are unified, one of them is converted
 * into a "redirect" pointing at the other. These redirects form a
 * DAG: the roots of the DAG (nodes that are not redirected) are each
 * associated with a value of type `V` and a rank. The rank is used
 * to keep the DAG relatively balanced, which helps keep the running
 * time of the algorithm under control. For more information, see
 * <http://en.wikipedia.org/wiki/Disjoint-set_data_structure>.
 */
@Suppress("UNCHECKED_CAST")
class UnificationTable<TyVar: TyInfer> {
    private val undoLog: MutableList<UndoLogEntry> = mutableListOf()

    private data class Root<TyVar: TyInfer>(
        val tyVar: TyVar,
        val varValue: VarValue
    ) {
    }

//    @Suppress("UNCHECKED_CAST")
//    private data class Root<out TyVarNode: Node>(
//        val key: TyVarNode
//    ) {
//        val value: Ty? get() = (key.parent as VarValue<Ty>).value
//    }

//    private fun setValue(root: Root<TyVar>, value: Ty) {
//        logNodeState(root.key)
//        root.key.parent = VarValue(value)
//    }

//    private fun unify(rootA: Root<TyVar>, rootB: Root<TyVar>, newValue: Ty?): TyVar {
//        logNodeState(rootA.key)
//        logNodeState(rootB.key)
//
//        // redirect roots
//        val oldRootKey = rootA.key
//        val newRootKey = rootB.key
//        oldRootKey.parent = newRootKey
//        newRootKey.parent = VarValue(newValue)
//        return newRootKey
//
////        return redirectRoot(rootA, rootB, newValue)
//    }

//    private fun redirectRoot(oldRoot: Root<TyVar>, newRoot: Root<TyVar>, newValue: Ty?): TyVar {
//        val oldRootKey = oldRoot.key
//        val newRootKey = newRoot.key
//        logNodeState(newRootKey)
//        logNodeState(oldRootKey)
//        oldRootKey.parent = newRootKey
//        newRootKey.parent = VarValue(newValue)
//        return newRootKey
//    }

    // root ty var is the one which has `?T => VarValue`
    fun resolveToRootTyVar(key: TyVar): TyVar = resolveToRoot(key).tyVar

    fun resolveTyInfer(key: TyVar): Ty? = resolveToRoot(key).varValue.ty

    fun unifyVarVar(leftVar: TyVar, rightVar: TyVar): TyVar {

        val leftRoot = this.resolveToRoot(leftVar)
        val rightRoot = this.resolveToRoot(rightVar)

        if (leftRoot.tyVar == rightRoot.tyVar) {
            // already unified
            return leftRoot.tyVar
        }

        logVarState(leftRoot.tyVar)
        logVarState(rightRoot.tyVar)

        val leftTy = leftRoot.varValue.ty
        val rightTy = rightRoot.varValue.ty

        val newValueTy = if (leftTy != null && rightTy != null) {
            // if  both vars are unified, their ty's must be the same
            if (leftTy != rightTy) {
                // must be solved on the upper level
                unificationError("Unification error: unifying $leftVar -> $rightVar")
            }
            leftTy
        } else {
            // use any non-null
            leftTy ?: rightTy
        }

        // redirect roots
        return redirectRoots(leftRoot, rightRoot, newValueTy)
//        val leftTyVar = leftRoot.tyVar
//        val rightTyVar = rightRoot.tyVar
//        leftTyVar.parent = rightTyVar
//        rightTyVar.parent = VarValue(newValueTy)

//        return rightRoot.tyVar
    }

    fun unifyVarValue(tyVar: TyVar, ty: Ty) {
        val oldTy = this.resolveTyInfer(tyVar)
        if (oldTy != null) {
            // if already unified, value must be the same
            if (oldTy != ty) {
                // must be solved on the upper level
                when (tyVar) {
                    is TyInfer.TyVar -> {
                        val origin = tyVar.origin?.origin
                        unificationError(
                            "unifying $tyVar -> $ty, old valueTy = $oldTy",
                            origin = origin
                        )
                    }
                    else -> unificationError("Unification error: unifying $tyVar -> $ty")
                }
            }
        }
        val rootEntry = resolveToRoot(tyVar)
        logVarState(rootEntry.tyVar)

        // set value
        setRootValueTy(rootEntry, ty)
//        rootEntry.tyVar.parent = VarValue(value)
    }

    private fun resolveToRoot(key: TyVar): Root<TyVar> {
        val keyParent = key.parent
        return when (keyParent) {
            is TyInfer -> {
//                val parentTyVar = keyParent as TyVar
//                val root = getRootEntry(parentTyVar)
//                if (key.parent != root.key) {
//                    logTyVarParent(key)
//                    key.parent = root.key // Path compression
//                }
                resolveToRoot(keyParent as TyVar)
            }
            is VarValue -> Root(key, keyParent)
        }
    }

    private fun setRootValueTy(rootEntry: Root<TyVar>, valueTy: Ty?) {
        rootEntry.tyVar.parent = VarValue(valueTy)
    }

    private fun redirectRoots(leftRoot: Root<TyVar>, rightRoot: Root<TyVar>, valueTy: Ty?): TyVar {
        // redirect roots
        val leftTyVar = leftRoot.tyVar
        val rightTyVar = rightRoot.tyVar
        leftTyVar.parent = rightTyVar
        rightTyVar.parent = VarValue(valueTy)
        return rightTyVar
    }

    private fun logVarState(tyVar: TyVar) {
        if (isSnapshot()) {
            undoLog.add(UndoLogEntry.SetOldParent(tyVar, tyVar.parent))
        }
    }

    private fun isSnapshot(): Boolean = undoLog.isNotEmpty()

    fun startSnapshot(): Snapshot {
        undoLog.add(UndoLogEntry.OpenSnapshot)
        return Snapshot(undoLog, undoLog.size - 1)
    }

//    private inner class SnapshotImpl(val position: Int): Snapshot {
//        override fun commit() {
//            assertOpenSnapshot(this)
//            if (position == 0) {
//                undoLog.clear()
//            } else {
//                undoLog[position] = UndoLogEntry.CommittedSnapshot
//            }
//        }
//
//        override fun rollback() {
//            val snapshotted = undoLog.subList(position + 1, undoLog.size)
//            snapshotted.reversed().forEach(UndoLogEntry::rollback)
//            snapshotted.clear()
//
//            val last = undoLog.removeAt(undoLog.size - 1)
//            check(last is UndoLogEntry.OpenSnapshot)
//            check(undoLog.size == position)
//        }
//
//        private fun assertOpenSnapshot(snapshot: SnapshotImpl) {
//            check(undoLog.getOrNull(snapshot.position) is UndoLogEntry.OpenSnapshot)
//        }
//    }
}

class Snapshot(val undoLog: MutableList<UndoLogEntry>, val position: Int) {
//    fun commit() {
//        assertOpenSnapshot(this)
//        if (position == 0) {
//            undoLog.clear()
//        } else {
//            undoLog[position] = UndoLogEntry.CommittedSnapshot
//        }
//    }

    fun rollback() {
        val snapshotted = undoLog.subList(position + 1, undoLog.size)
        snapshotted.reversed().forEach {
            (it as UndoLogEntry.SetOldParent).rollback()
        }
//        snapshotted.reversed().forEach(UndoLogEntry::rollback)
        snapshotted.clear()

        val last = undoLog.removeAt(undoLog.size - 1)
        check(last is UndoLogEntry.OpenSnapshot)
        check(undoLog.size == position)
    }

//    private fun assertOpenSnapshot(snapshot: Snapshot) {
//        check(undoLog.getOrNull(snapshot.position) is UndoLogEntry.OpenSnapshot)
//    }
}


fun unificationError(message: String, origin: MvTypeParameter? = null): Nothing {
    if (origin == null) {
        throw UnificationError(message, origin = null)
    }
    val file = origin.containingFile
    val error = if (file != null) {
        UnificationError(message, origin = PsiErrorContext(origin.text, file, file.elementLocation(origin)))
    } else {
        UnificationError(message, PsiErrorContext(origin.text, null, null))
    }
    throw error
}

data class PsiErrorContext(val text: String, val file: PsiFile?, val location: PsiLocation?) {
    override fun toString(): String {
        return "${text.quote()} \n(at ${file?.toNioPathOrNull()} $location)"
    }

    companion object {
        fun fromElement(element: PsiElement): PsiErrorContext {
            val elementText = element.text
            val file = element.containingFile
            return if (file != null) {
                PsiErrorContext(elementText, file, file.elementLocation(element))
            } else {
                PsiErrorContext(elementText, null, null)
            }
        }
    }
}

class UnificationError(
    message: String,
    val origin: PsiErrorContext? = null,
    var context: PsiErrorContext? = null,
):
    IllegalStateException(message) {

    override fun toString(): String {
        @Suppress("DEPRECATION")
        var message = super.toString()
        val origin = origin
        if (origin != null) {
            message += ", \ntype parameter origin: \n$origin"
        }
        val context = context
        if (context != null) {
            message += ", \ncontext: \n$context"
        }
//        val combine = combine
//        if (combine != null) {
//            message += ", \ncombine types: ${combine.ty1} <-> ${combine.ty2}"
//        }
        return message
    }
}

//interface Snapshot {
//    fun commit()
//
//    fun rollback()
//}

sealed class UndoLogEntry {
//    abstract fun rollback()

    object OpenSnapshot: UndoLogEntry() {
//        override fun rollback() {
//            unificationError("Cannot rollback an uncommitted snapshot")
//        }
    }

    data class SetOldParent(val tyVar: TyInfer, val oldParent: VarOrValue): UndoLogEntry() {
        fun rollback() {
            tyVar.parent = oldParent
        }
    }
}

//class CombinedSnapshot(private vararg val snapshots: Snapshot) {
//    fun rollback() = snapshots.forEach { it.rollback() }
////    fun commit() = snapshots.forEach { it.commit() }
//}
