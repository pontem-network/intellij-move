/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString


object TyUnknown : Ty() {
    override fun abilities() = Ability.all()
    override fun toString(): String = tyToString(this)
}
