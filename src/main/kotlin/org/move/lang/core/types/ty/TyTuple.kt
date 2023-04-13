/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString

import org.move.lang.core.types.infer.TypeFolder
import org.move.lang.core.types.infer.TypeVisitor

data class TyTuple(val types: List<Ty>) : Ty() {
    override fun abilities() = Ability.all()

    override fun innerFoldWith(folder: TypeFolder): Ty =
        TyTuple(types.map { it.foldWith(folder) })

    override fun innerVisitWith(visitor: TypeVisitor): Boolean =
        types.any(visitor)

    override fun toString(): String = tyToString(this)
}
