package org.move.cli.project

import com.intellij.openapi.vfs.VirtualFile
import org.move.cli.DeclaredAddresses

abstract class Package(open val contentRoot: VirtualFile) {

    abstract fun addresses(): DeclaredAddresses
}
