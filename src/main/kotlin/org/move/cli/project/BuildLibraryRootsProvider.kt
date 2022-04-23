package org.move.cli.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VirtualFile
import org.move.cli.MvConstants
import org.move.openapiext.contentRoots

class BuildLibraryRootsProvider : AdditionalLibraryRootsProvider() {
    override fun getAdditionalProjectLibraries(project: Project): MutableSet<SyntheticLibrary> {
        val libs = mutableSetOf<SyntheticLibrary>()
        val contentRoot = project.contentRoots.singleOrNull() ?: return libs
        if (contentRoot.findChild(MvConstants.MANIFEST_FILE) != null) {
            contentRoot.findChild("build")
                ?.let { libs.add(SyntheticLibrary.newImmutableLibrary(listOf(it))) }
        }
        return libs
    }

    override fun getRootsToWatch(project: Project): List<VirtualFile> {
        return getAdditionalProjectLibraries(project).flatMap { it.sourceRoots }
    }
}

private fun <U, V> Collection<U>.smartFlatMap(transform: (U) -> Collection<V>): Collection<V> =
    when (size) {
        0 -> emptyList()
        1 -> transform(first())
        else -> this.flatMap(transform)
    }
