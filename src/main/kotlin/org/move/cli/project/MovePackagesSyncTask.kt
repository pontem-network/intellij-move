package org.move.cli.project

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import org.move.cli.Consts
import org.move.lang.toNioPathOrNull
import org.move.lang.toTomlFile
import org.move.openapiext.contentRoots
import org.move.openapiext.resolveExisting
import org.move.openapiext.toVirtualFile
import org.move.stdext.iterateFiles
import java.util.concurrent.CompletableFuture

class MovePackagesSyncTask(
    project: Project,
    private val future: CompletableFuture<List<MovePackage>>
) : Task.Backgroundable(project, "Reloading Move packages", true) {

    override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = true

        // aptos move fetch
        fetchDependencies()

        val packages = loadPackages(project)
        future.complete(packages)
    }

    private fun fetchDependencies() {
        // run `aptos move fetch` here
    }

    companion object {
        fun loadPackages(project: Project): List<MovePackage> {
            val packages = mutableListOf<MovePackage>()
            val moveTomlQueue = ArrayDeque<MoveToml>()
            for (contentRoot in project.contentRoots) {
                contentRoot.iterateFiles({ it.name == Consts.MANIFEST_FILE }) {
                    val root = it.parent?.toNioPathOrNull() ?: return@iterateFiles true
                    val tomlFile = it.toTomlFile(project) ?: return@iterateFiles true
                    val moveToml = MoveToml.fromTomlFile(tomlFile, root)
                    moveTomlQueue.add(moveToml)
                    true
                }
            }
            while (moveTomlQueue.isNotEmpty()) {
                val moveToml = moveTomlQueue.removeFirst()
                for ((dep, _) in moveToml.deps) {
                    val root = dep.localPath()
                    val tomlFile = root
                        .resolveExisting(Consts.MANIFEST_FILE)
                        ?.toVirtualFile()
                        ?.toTomlFile(project) ?: continue
                    val depMoveToml = MoveToml.fromTomlFile(tomlFile, root)
                    moveTomlQueue.addLast(depMoveToml)
                }
                val movePackage = MovePackage.fromMoveToml(moveToml) ?: continue
                packages.add(movePackage)
            }
            return packages
        }
    }
}
