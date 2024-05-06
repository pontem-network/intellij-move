package org.move.cli.externalSystem

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.externalSystem.autoimport.AutoImportProjectNotificationAware
import com.intellij.openapi.externalSystem.autoimport.AutoImportProjectTracker
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.PathUtil
import org.move.cli.MoveProjectsService
import org.move.lang.core.psi.MvPath
import org.move.utils.tests.MvProjectTestBase
import org.move.utils.tests.TestProject
import org.move.utils.tests.waitFinished
import java.io.IOException
import java.util.concurrent.CountDownLatch

@Suppress("UnstableApiUsage")
class MoveExternalSystemProjectAwareTest: MvProjectTestBase() {

    private val notificationAware get() = AutoImportProjectNotificationAware.getInstance(project)
    private val projectTracker get() = AutoImportProjectTracker.getInstance(project)

    private val moveSystemId: ExternalSystemProjectId
        get() = projectTracker
            .getActivatedProjects()
            .first { it.systemId == MoveExternalSystemProjectAware.MOVE_SYSTEM_ID }

    override fun setUp() {
        super.setUp()
        AutoImportProjectTracker.enableAutoReloadInTests(testRootDisposable)
    }

    fun `test modifications`() {
        val testProject = testProject {
            namedMoveToml("RootPackage")
            build {
                dir("RootPackage") {
                    buildInfoYaml("""
---
compiled_package_info:
  package_name: RootPackage
""")
                }
            }
            sources {
                main(
                    """
                        module 0x1::main { /*caret*/ }                         
                    """
                )
            }
            dir("child1") {
                namedMoveToml("ChildPackage1")
                sources {
                    main(
                        """
                        module 0x1::main {}                         
                    """
                    )
                }
            }
            dir("child2") {
                namedMoveToml("ChildPackage2")
                sources {
                    main(
                        """
                        module 0x1::main {}                         
                    """
                    )
                }
            }
        }

        assertNotificationAware(event = "after project creation")

        testProject.checkFileModification("Move.toml", triggered = true)
        testProject.checkFileModification("child1/Move.toml", triggered = true)
        testProject.checkFileModification("child2/Move.toml", triggered = true)

        testProject.checkFileModification("sources/main.move", triggered = false)
        testProject.checkFileModification("child1/sources/main.move", triggered = false)
        testProject.checkFileModification("child2/sources/main.move", triggered = false)
    }

    fun `test reload`() {
        val testProject = testProject {
            moveToml("""
                [package]
                name = "MainPackage"
                
                [dependencies]
                #Dep = { local = "./dep" }
            """)
            sources {
                main("""
                    module 0x1::main {
                        fun main() {
                            0x1::dep::call();
                                    //^
                        }
                    }                    
                """)
            }
            dir("dep") {
                namedMoveToml("Dep")
                sources {
                    move("dep.move", """
                        module 0x1::dep {
                            public fun call() {}
                        }                        
                    """)
                }
            }
        }
        assertNotificationAware(event = "initial project creation")

        testProject.checkReferenceIsResolved<MvPath>("sources/main.move", shouldNotResolve = true)

        val moveToml = testProject.file("Move.toml")
        runWriteAction {
            VfsUtil.saveText(moveToml, VfsUtil.loadText(moveToml).replace("#", ""))
        }
        assertNotificationAware(moveSystemId, event = "modification in Cargo.toml")

        PsiDocumentManager.getInstance(project).commitAllDocuments()

        scheduleProjectReload()
        assertNotificationAware(event = "project reloading")

        runWithInvocationEventsDispatching("Failed to resolve the reference") {
            testProject.findElementInFile<MvPath>("sources/main.move").reference?.resolve() != null
        }
    }

    private fun TestProject.checkFileModification(path: String, triggered: Boolean) {
        val file = file(path)
        val initialText = VfsUtil.loadText(file)
        checkModification("modifying of", path, triggered,
                          apply = { VfsUtil.saveText(file, "$initialText\nsome text") },
                          revert = { VfsUtil.saveText(file, initialText) }
        )
    }

    private fun TestProject.checkFileDeletion(path: String, triggered: Boolean) {
        val file = file(path)
        val initialText = VfsUtil.loadText(file)
        checkModification("removing of ", path, triggered,
                          apply = { file.delete(file.fileSystem) },
                          revert = { createFile(rootDirectory, path, initialText) }
        )
    }

    private fun TestProject.checkFileCreation(path: String, triggered: Boolean) {
        checkModification("creation of", path, triggered,
                          apply = { createFile(rootDirectory, path) },
                          revert = {
                              val file = file(path)
                              file.delete(file.fileSystem)
                          }
        )
    }

    private fun checkModification(
        eventName: String,
        path: String,
        triggered: Boolean,
        apply: () -> Unit,
        revert: () -> Unit
    ) {
        runWriteAction {
            apply()
        }
        val externalSystems = if (triggered) arrayOf(moveSystemId) else arrayOf()
        assertNotificationAware(*externalSystems, event = "$eventName $path")

        runWriteAction {
            revert()
        }
        assertNotificationAware(event = "revert $eventName $path")
    }

    private fun scheduleProjectReload() {
        val newDisposable = Disposer.newDisposable()
        val startLatch = CountDownLatch(1)
        val endLatch = CountDownLatch(1)
        project.messageBus.connect(newDisposable).subscribe(
            MoveProjectsService.MOVE_PROJECTS_REFRESH_TOPIC,
            object: MoveProjectsService.MoveProjectsRefreshListener {
                override fun onRefreshStarted() {
                    startLatch.countDown()
                }

                override fun onRefreshFinished(status: MoveProjectsService.MoveRefreshStatus) {
                    endLatch.countDown()
                }
            }
        )

        try {
            projectTracker.scheduleProjectRefresh()

            if (!startLatch.waitFinished(1000)) error("Move project reloading hasn't started")
            if (!endLatch.waitFinished(5000)) error("Move project reloading hasn't finished")
        } finally {
            Disposer.dispose(newDisposable)
        }
    }

    private fun assertNotificationAware(vararg projects: ExternalSystemProjectId, event: String) {
        val message =
            if (projects.isEmpty()) "Notification must be expired" else "Notification must be notified"
        assertEquals("$message on $event", projects.toSet(), notificationAware.getProjectsWithNotification())
    }

    @Throws(IOException::class)
    private fun createFile(root: VirtualFile, path: String, text: String = ""): VirtualFile {
        val name = PathUtil.getFileName(path)
        val parentPath = PathUtil.getParentPath(path)
        var parent = root
        if (parentPath.isNotEmpty()) {
            parent = VfsUtil.createDirectoryIfMissing(root, parentPath) ?: error("Failed to create $parentPath directory")
        }
        val file = parent.createChildData(parent.fileSystem, name)
        VfsUtil.saveText(file, text)
        return file
    }
}