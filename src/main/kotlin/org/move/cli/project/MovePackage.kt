package org.move.cli.project

import com.intellij.openapi.vfs.VirtualFile

enum class PackageOrigin {
    WORKSPACE,
    LOCAL_DEP,
    GIT_DEP,
}

data class MovePackage(val contentRoot: VirtualFile, val origin: PackageOrigin)

//class Package {
//
//
//    enum class Origin {
//        WORKSPACE,
//        LOCAL_DEPENDENCY,
//        GIT_DEPENDENCY,
//    }
//}
