package org.move.lang.core.resolve2.util

import org.move.lang.core.psi.MvPath
import org.move.lang.core.psi.MvUseAlias
import org.move.lang.core.psi.MvUseSpeck
import org.move.lang.core.psi.MvUseStmt
import org.move.lang.core.psi.ext.childOfType
import org.move.lang.core.psi.ext.childrenOfType
import org.move.lang.core.psi.ext.hasColonColon

fun interface LeafUseSpeckConsumer {
    fun consume(path: MvPath, useAlias: MvUseAlias?): Boolean
}

fun MvUseStmt.forEachLeafSpeck(consumer: LeafUseSpeckConsumer) {
    val rootUseSpeck = this.childOfType<MvUseSpeck>() ?: return
    val useGroup = rootUseSpeck.useGroup
    if (useGroup == null) {
        // basePath is null, path is full path of useSpeck
        val alias = rootUseSpeck.useAlias
        if (!consumer.consume(rootUseSpeck.path, alias)) return
    } else {
        for (childSpeck in useGroup.childrenOfType<MvUseSpeck>()) {
            val alias = childSpeck.useAlias
            if (!consumer.consume(childSpeck.path, alias)) return
        }
    }
}




///** return false if path is incomplete (has empty segments), e.g. `use std::;` */
//private fun addPathSegments(path: MvPath, segments: ArrayList<String>): Boolean {
//    val subpath = path.path
//    if (subpath != null) {
//        if (!addPathSegments(subpath, segments)) return false
//    } else if (path.hasColonColon) {
//        // absolute path: `::foo::bar`
//        //                 ~~~~~ this
//        segments += ""
//    }
//
//    val segment = path.referenceName ?: return false
//    segments += segment
//    return true
//}
