/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.utils.tests

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.intellij.lang.annotations.Language
import org.move.lang.core.psi.MoveReferenceElement
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.openapiext.document
import org.move.openapiext.fullyRefreshDirectory
import org.move.openapiext.toPsiFile
import org.move.utils.tests.resolve.TestResolveResult
import org.move.utils.tests.resolve.checkResolvedFile

fun fileTree(builder: FileTreeBuilder.() -> Unit): FileTree =
    FileTree(FileTreeBuilderImpl().apply { builder() }.intoDirectory())

fun fileTreeFromText(@Language("Move") text: String): FileTree {
    val fileSeparator = """^\s*//- (\S+)\s*$""".toRegex(RegexOption.MULTILINE)
    val fileNames = fileSeparator.findAll(text).map { it.groupValues[1] }.toList()
    val fileTexts = fileSeparator.split(text)
        // cleans up from the first blank text
        .let {
            check(it.first().isBlank())
            it.drop(1)
        }
        .map { it.trimIndent() }

    check(fileNames.size == fileTexts.size) {
        "Have you placed `//- filename.rs` markers?"
    }

    fun fillDirectory(dir: FilesystemEntry.Directory, path: List<String>, contents: String) {
        val name = path.first()
        if (path.size == 1) {
            dir.children[name] = FilesystemEntry.File(contents)
        } else {
            val childDir =
                dir.children.getOrPut(name) { FilesystemEntry.Directory(mutableMapOf()) } as FilesystemEntry.Directory
            fillDirectory(childDir, path.drop(1), contents)
        }
    }

    val filesInfo =
        FilesystemEntry.Directory(mutableMapOf()).apply {
            val dirFiles = fileNames.map { it.split("/") }.zip(fileTexts)
            for ((path, contents) in dirFiles) {
                fillDirectory(this, path, contents)
            }
        }
    return FileTree(filesInfo)
}

interface FileTreeBuilder {
    fun dir(name: String, builder: FileTreeBuilder.() -> Unit)
    fun dir(name: String, tree: FileTree)
    fun file(name: String, code: String)

    fun move(name: String, @Language("Move") code: String) = file(name, code)
    fun toml(name: String, @Language("TOML") code: String) = file(name, code)
}

class FileTree(val rootDirInfo: FilesystemEntry.Directory) {
    fun prepareTestProject(project: Project, directory: VirtualFile): TestProject {
        val filesWithCaret: MutableList<String> = mutableListOf()
        val filesWithNamedElement: MutableList<String> = mutableListOf()

        fun prepareFilesFromInfo(
            dirInfo: FilesystemEntry.Directory,
            root: VirtualFile,
            parentComponents: List<String> = emptyList()
        ) {
            for ((name, fsEntry) in dirInfo.children) {
                val pathComponents = parentComponents + name
                when (fsEntry) {
                    is FilesystemEntry.File -> {
                        val vFile = root.findChild(name) ?: root.createChildData(root, name)
                        VfsUtil.saveText(vFile, replaceCaretMarker(fsEntry.text))
                        if (hasCaretMarker(fsEntry.text) || "//^" in fsEntry.text) {
                            filesWithCaret += pathComponents.joinToString(separator = "/")
                        }
                        if ("//X" in fsEntry.text) {
                            filesWithNamedElement += pathComponents.joinToString(separator = "/")
                        }
                    }
                    is FilesystemEntry.Directory -> {
                        prepareFilesFromInfo(fsEntry, root.createChildDirectory(root, name), pathComponents)
                    }
                }
            }
        }

        runWriteAction {
            prepareFilesFromInfo(rootDirInfo, directory)
            fullyRefreshDirectory(directory)
        }
        return TestProject(project, directory, filesWithCaret, filesWithNamedElement)
    }
}

fun FileTree.prepareTestProject(fixture: CodeInsightTestFixture): TestProject =
    prepareTestProject(fixture.project, fixture.findFileInTempDir("."))

fun FileTree.createAndOpenFileWithCaretMarker(fixture: CodeInsightTestFixture): TestProject {
    val testProject = prepareTestProject(fixture)
    fixture.configureFromTempProjectFile(testProject.fileWithCaret)
    return testProject
}

class TestProject(
    private val project: Project,
    val root: VirtualFile,
    private val filesWithCaret: List<String>,
    private val filesWithNamedElement: List<String>
) {

    val fileWithCaret: String get() = filesWithCaret.single()
    val fileWithNamedElement: String get() = filesWithNamedElement.single()

    inline fun <reified T : PsiElement> findElementInFile(path: String): T {
        val element = doFindElementInFile(path)
        return element.ancestorStrict()
            ?: error("No parent of type ${T::class.java} for ${element.text}")
    }

    inline fun <reified T : MoveReferenceElement> checkReferenceIsResolved(
        path: String,
        shouldNotResolve: Boolean = false,
//        toCrate: String? = null,
        toFile: String? = null
    ) {
        val ref = findElementInFile<T>(path)
//        val reference = ref.reference ?: error("Failed to get reference for `${ref.text}`")
        val res = ref.reference?.resolve()
        if (shouldNotResolve) {
            check(res == null) {
                "Reference ${ref.text} should be unresolved in `$path`"
            }
        } else {
            check(res != null) {
                "Failed to resolve the reference `${ref.text}` in `$path`."
            }
//            if (toCrate != null) {
//                val pkg = res.containingCargoPackage?.let { "${it.name} ${it.version}" } ?: "[nowhere]"
//                check(pkg == toCrate) {
//                    "Expected to be resolved to $toCrate but actually resolved to $pkg"
//                }
//            }
            if (toFile != null) {
                val file = res.containingFile.virtualFile
                val result = checkResolvedFile(file, toFile) { file.fileSystem.findFileByPath(it) }
                check(result !is TestResolveResult.Err) {
                    (result as TestResolveResult.Err).message
                }
            }
        }
    }

    fun doFindElementInFile(path: String): PsiElement {
        val vFile = root.findFileByRelativePath(path)
            ?: error("No `$path` file in test project")
        val file = vFile.toPsiFile(project)!!
        return findElementInFile(file, "^")
    }

    fun psiFile(path: String): PsiFileSystemItem {
        val vFile = root.findFileByRelativePath(path)
            ?: error("Can't find `$path`")
        val psiManager = PsiManager.getInstance(project)
        return if (vFile.isDirectory) psiManager.findDirectory(vFile)!! else psiManager.findFile(vFile)!!
    }
}


private class FileTreeBuilderImpl(
    val directory: MutableMap<String, FilesystemEntry> = mutableMapOf()
) : FileTreeBuilder {

    override fun dir(name: String, builder: FileTreeBuilder.() -> Unit) {
        check('/' !in name) { "Bad directory name `$name`" }
        directory[name] = FileTreeBuilderImpl().apply { builder() }.intoDirectory()
    }

    override fun dir(name: String, tree: FileTree) {
        check('/' !in name) { "Bad directory name `$name`" }
        directory[name] = tree.rootDirInfo
    }

    override fun file(name: String, code: String) {
        check('/' !in name && '.' in name) { "Bad file name `$name`" }
        directory[name] = FilesystemEntry.File(code.trimIndent())
    }

    fun intoDirectory() = FilesystemEntry.Directory(directory)
}

sealed class FilesystemEntry {
    class File(val text: String) : FilesystemEntry()
    class Directory(val children: MutableMap<String, FilesystemEntry>) : FilesystemEntry()
}

private fun findElementInFile(file: PsiFile, marker: String): PsiElement {
    val markerOffset = file.text.indexOf(marker)
    check(markerOffset != -1) { "No `$marker` in \n${file.text}" }

    val doc = file.document!!
    val markerLine = doc.getLineNumber(markerOffset)
    val makerColumn = markerOffset - doc.getLineStartOffset(markerLine)
    val elementOffset = doc.getLineStartOffset(markerLine - 1) + makerColumn

    return file.findElementAt(elementOffset) ?: error { "No element found, offset = $elementOffset" }
}

fun replaceCaretMarker(text: String): String = text.replace("/*caret*/", "<caret>")
fun hasCaretMarker(text: String): Boolean = text.contains("/*caret*/") || text.contains("<caret>")
