package org.move.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VirtualFile
import org.move.lang.core.psi.ext.wrapWithList
import org.move.lang.core.psi.ext.wrapWithMutableList
import org.move.lang.toNioPathOrNull
import org.move.openapiext.contentRoots

class BuildDirectoryLibraryProvider : AdditionalLibraryRootsProvider() {
    override fun getRootsToWatch(project: Project): MutableCollection<VirtualFile> {
        val buildDir = project.contentRoots.singleOrNull()?.findChild("build")
        return buildDir.wrapWithMutableList()
    }

    override fun getAdditionalProjectLibraries(project: Project): MutableCollection<SyntheticLibrary> {
        val libs = mutableListOf<SyntheticLibrary>()
        val contentRoot = project.contentRoots.singleOrNull() ?: return libs
        val manifestFile = contentRoot.findChild("Move.toml") ?: return libs

        if (manifestFile.exists() && manifestFile.toNioPathOrNull() != null) {
//            val moveProject = project.moveProjects.findProjectForPath(manifestFile.toNioPath()) ?: return libs
//            val gitDepNames = moveProject.moveToml.dependencies.asDependenciesMap()
//                .filter { (name, dep) -> dep is Dependency.Git }.map { it.key }
            val buildDir = contentRoot.findChild("build") ?: return libs
            libs.add(
                SyntheticLibrary.newImmutableLibrary(buildDir.wrapWithList())
//                SyntheticLibrary.newImmutableLibrary(gitDepNames.mapNotNull { buildDir.findChild(it) })
            )
        }
        return libs
    }
}
