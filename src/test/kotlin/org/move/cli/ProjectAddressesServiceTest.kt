package org.move.cli

import com.intellij.openapi.project.rootManager
import org.move.openapiext.toPsiFile
import org.move.utils.tests.MoveProjectTestCase
import org.move.utils.tests.fileTree

class ProjectAddressesServiceTest: MoveProjectTestCase() {
    fun `test directory index cached correctly`() {
        val fileTree = fileTree {
            toml("Move.toml", "")
            dir("sources") {
                move("module.move", "")
            }
            dir("stdlib") {
                toml("Move.toml", "")
                dir("sources") {
                    move("module2.move", "")
                }
            }
        }
        val rootDirectory = myModule.rootManager.contentRoots.first()
        val testProject = fileTree.prepareTestProject(myFixture.project, rootDirectory)
        val addressesService = project.moveProjectsService
//        addressesService.refreshProjectAddresses()

        val moveFiles = findMoveTomlFilesDeepestFirst(myFixture.project).toList()
        check(moveFiles.size == 2)
        check(moveFiles[0].path.endsWith("stdlib/Move.toml")) { moveFiles[0].path }
        check(moveFiles[1].path.endsWith("Move.toml")) { moveFiles[1].path }

        val vfile = testProject.psiFile("stdlib/sources/module2.move").virtualFile.toPsiFile(project)!!
        println("moveTomlPath: ${addressesService.findMoveProjectForPsiFile(vfile)?.root}")
        println("moveTomlPath: ${addressesService.findMoveProjectForPsiFile(vfile)?.root}")
        println("moveTomlPath: ${addressesService.findMoveProjectForPsiFile(vfile)?.root}")
    }
}
