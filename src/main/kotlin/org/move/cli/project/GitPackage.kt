package org.move.cli.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.cli.DeclaredAddresses
import org.move.cli.placeholderMap

data class GitPackage(
    override val contentRoot: VirtualFile,
    private val project: Project,
    val buildInfoYaml: BuildInfoYaml,
) : Package(contentRoot) {

    override fun addresses() = DeclaredAddresses(buildInfoYaml.addresses(), placeholderMap())
}
