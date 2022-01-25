package org.move.cli

import org.move.openapiext.toPsiFile
import org.move.utils.tests.MvProjectTestCase

class ProjectAddressesServiceTest: MvProjectTestCase() {
    fun `test directory index cached correctly`() {
        val testProject = testProjectFromFileTree {
            moveToml()
            sources {
                move("module.move")
            }
            dir("stdlib") {
                moveToml()
                sources {
                    move("module2.move")
                }
            }
        }
        val addressesService = project.moveProjects

        val moveFiles = findMoveTomlFilesDeepestFirst(myFixture.project).toList()
        check(moveFiles.size == 2)
        check(moveFiles[0].path.endsWith("stdlib/Move.toml")) { moveFiles[0].path }
        check(moveFiles[1].path.endsWith("Move.toml")) { moveFiles[1].path }

        val vfile = testProject.psiFile("stdlib/sources/module2.move").virtualFile.toPsiFile(project)!!
        println("moveTomlPath: ${addressesService.findProjectForPsiElement(vfile)?.root}")
        println("moveTomlPath: ${addressesService.findProjectForPsiElement(vfile)?.root}")
        println("moveTomlPath: ${addressesService.findProjectForPsiElement(vfile)?.root}")
    }
}
