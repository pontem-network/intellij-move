package org.move.utils.tests.resolve

import com.intellij.openapi.project.rootManager
import org.intellij.lang.annotations.Language
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MoveReferenceElement
import org.move.openapiext.findVirtualFile
import org.move.utils.tests.MoveHeavyTestBase
import org.move.utils.tests.base.findElementInEditor
import org.move.utils.tests.base.findElementWithDataAndOffsetInEditor
import org.move.utils.tests.fileTreeFromText

abstract class ResolveHeavyTestCase : MoveHeavyTestBase() {
    protected fun checkByFileTree(@Language("Move") code: String) {
        val fileTree = fileTreeFromText(code)
        val rootDirectory = myModule.rootManager.contentRoots.first()

        val testProject = fileTree.prepareTestProject(myFixture.project, rootDirectory)
        val fileWithCaret =
            rootDirectory.toNioPath().resolve(testProject.fileWithCaret).findVirtualFile()
                ?: error("No file with //^ caret")
        myFixture.configureFromExistingVirtualFile(fileWithCaret)

        val (refElement, data, offset) =
            myFixture.findElementWithDataAndOffsetInEditor<MoveReferenceElement>("^")
        if (data == "unresolved") {
            val resolved = refElement.reference?.resolve()
            check(resolved == null) {
                "$refElement `${refElement.text}`should be unresolved, was resolved to\n$resolved `${resolved?.text}`"
            }
            return
        }
        val resolved = refElement.checkedResolve(offset)

        val fileWithNamedElement =
            rootDirectory.toNioPath()
                .resolve(testProject.fileWithNamedElement).findVirtualFile()
                ?: error("No file with //X caret")
        myFixture.configureFromExistingVirtualFile(fileWithNamedElement)
        val target = myFixture.findElementInEditor(MoveNamedElement::class.java, "X")
        check(resolved == target) {
            "$refElement `${refElement.text}` should resolve to $target (${target.text}), was $resolved (${resolved.text}) instead"
        }
    }
}
