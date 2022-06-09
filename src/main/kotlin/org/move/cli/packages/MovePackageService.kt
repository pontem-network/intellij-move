package org.move.cli.packages

import com.intellij.execution.RunManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.messages.Topic
import org.move.cli.*
import org.move.lang.toNioPathOrNull
import org.move.openapiext.common.isUnitTestMode
import org.move.openapiext.toVirtualFile
import org.move.stdext.AsyncValue
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

val Project.packageService: MovePackageService get() = service()

fun setupProjectRoots(project: Project, movePackages: List<MovePackage>) {
    invokeAndWaitIfNeeded {
        // Initialize services that we use (probably indirectly) in write action below.
        // Otherwise, they can be initialized in write action that may lead to deadlock
        RunManager.getInstance(project)
        ProjectFileIndex.getInstance(project)

        runWriteAction {
//            if (project.isDisposed) return@runWriteAction
            ProjectRootManagerEx.getInstanceEx(project)
                .mergeRootsChangesDuring {
                    for (movePackage in movePackages) {
                        val contentRoot = movePackage.contentRoot
                        val movePackageModule =
                            ModuleUtilCore.findModuleForFile(contentRoot, project) ?: continue
                        ModuleRootModificationUtil.updateModel(movePackageModule) {
                            // exclude build/ directory from indexing
                            val contentEntry = it.contentEntries.singleOrNull() ?: return@updateModel
                            val buildPath = FileUtil.join(contentRoot.url, "build")
                            contentEntry.addExcludeFolder(buildPath)
                        }
                    }
                }
        }
    }
}

class MovePackageService(val project: Project) {
    private var initialized = false
    private val packages: AsyncValue<List<MovePackage>> = AsyncValue(emptyList())

    private val packageIndex: PackagesFileIndex = PackagesFileIndex(project)

    fun findMovePackage(psiElement: PsiElement): MovePackage? {
        val file = when (psiElement) {
            is PsiDirectory -> psiElement.virtualFile
            is PsiFile -> psiElement.originalFile.virtualFile
            else -> psiElement.containingFile?.originalFile?.virtualFile
        } ?: return null
        return findMovePackageForFile(file)
    }

    fun findMovePackage(path: Path): MovePackage? {
        val file = path.toVirtualFile() ?: return null
        return findMovePackageForFile(file)
    }

    fun refresh() {
        initialized = true
        LOG.info("MovePackagesService state refresh started")
        packages
            .updateAsync { doRefresh() }
            .thenApply(::resetIDEState)
    }

    private fun doRefresh(): CompletableFuture<List<MovePackage>> {
        val future = CompletableFuture<List<MovePackage>>()
        val packagesSyncTask = MovePackagesSyncTask(project, future)
        project.taskQueue.run(packagesSyncTask)
//        packagesSyncTask
//            .setCancelText("Stop")
//            .queue()
        return future.thenApply { packages ->
            runOnlyInNonLightProject(project) {
                setupProjectRoots(project, packages)
            }
            packages
        }
    }

    private fun resetIDEState(packages: List<MovePackage>) {
        invokeAndWaitIfNeeded {
            runWriteAction {
//                directoryIndex.resetIndex()
                // In unit tests roots change is done by the test framework in most cases
                runOnlyInNonLightProject(project) {
                    ProjectRootManagerEx.getInstanceEx(project)
                        .makeRootsChange(EmptyRunnable.getInstance(), false, true)
                }
//                project.messageBus
//                    .syncPublisher(MOVE_PACKAGES_CHANGES_TOPIC)
//                    .movePackagesUpdated(this, packages)
            }
        }
    }

    private fun findMovePackageForFile(file: VirtualFile): MovePackage? {
        check(initialized) {
            "Packages state is not yet initialized"
        }
        val cachedPackageEntry = this.packageIndex.get(file)
        if (cachedPackageEntry is PackageFileIndexEntry.Present) {
            return cachedPackageEntry.package_
        }

        val filePath = file.toNioPathOrNull() ?: return null
        var maxDepth = 0
        var movePackage: MovePackage? = null
        for (package_ in this.packages.state) {
            if (package_.layout().any { filePath.startsWith(it) }) {
                val depth = package_.contentRoot.fsDepth
                if (maxDepth < depth) {
                    maxDepth = depth
                    movePackage = package_
                }
            }
        }
        if (movePackage == null && isUnitTestMode) {
            // this is for light tests, heavy test should always have valid moveProject
            movePackage = testEmptyMovePackage(project)
        }
        this.packageIndex.put(file, PackageFileIndexEntry.Present(movePackage))
        return movePackage
    }

    companion object {
        private val LOG = logger<MovePackageService>()

//        fun interface MovePackagesUpdatedListener {
//            fun movePackagesUpdated(
//                service: MovePackageService,
//                packages: List<MovePackage>
//            )
//        }

//        private val MOVE_PACKAGES_CHANGES_TOPIC = Topic(
//            "packages changes",
//            MovePackagesUpdatedListener::class.java
//        )

    }
}
