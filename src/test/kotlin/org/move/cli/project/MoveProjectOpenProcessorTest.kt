package org.move.cli.project

import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import org.move.cli.MoveProjectOpenProcessor
import org.move.utils.tests.MvProjectTestBase
import org.move.utils.tests.fileTree

class MoveProjectOpenProcessorTest : MvProjectTestBase() {
    fun `test open project via directory with Move toml file`() {
        val testDir = fileTree {
            dir("package") {
                namedMoveToml("MyPackage")
                sources { }
            }
        }.prepareTestProject(project, myModule.rootManager.sourceRoots.first())

        val packageDir = testDir.file("package")
        checkFileCanBeOpenedAsProject(packageDir)
    }

    private fun checkFileCanBeOpenedAsProject(file: VirtualFile) {
        val processor =
            ProjectOpenProcessor.EXTENSION_POINT_NAME.extensionList.find { it is MoveProjectOpenProcessor }
                ?: error("MoveProjectOpenProcessor is not registered in plugin.xml")
        check(processor.canOpenProject(file)) {
            "Move project cannot be opened via `$file`"
        }
    }
}
