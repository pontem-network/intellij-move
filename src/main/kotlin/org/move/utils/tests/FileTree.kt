/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

@file:Suppress("FunctionName")

package org.move.utils.tests

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import org.intellij.lang.annotations.Language
import org.move.cli.manifest.TomlDependency
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.resolve.ref.MvReferenceElement
import org.move.openapiext.document
import org.move.openapiext.fullyRefreshDirectory
import org.move.openapiext.toPsiFile
import org.move.utils.tests.resolve.TestResolveResult
import org.move.utils.tests.resolve.checkResolvedFile

typealias TreeBuilder = FileTreeBuilder.() -> Unit

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
    fun dir(name: String, builder: TreeBuilder)
    fun dir(name: String, tree: FileTree)
    fun file(name: String, code: String)

    fun main(@Language("Move") code: String = "") = move("main.move", code)
    fun move(name: String, @Language("Move") code: String = "") = file(name, code)
    fun toml(name: String, @Language("TOML") code: String = "") = file(name, code)

    fun moveToml(@Language("TOML") code: String = "") = file("Move.toml", code)
    fun namedMoveToml(packageName: String) = moveToml(
        """
    [package]
    name = "$packageName"    
    """
    )

    fun git(repo: String, rev: String, builder: TreeBuilder = {}) {
        val dirName = TomlDependency.Git.dirName(repo, rev)
        return dir(dirName, builder)
    }

    fun dotMove(builder: TreeBuilder = {}) = dir(".move", builder)

    fun buildInfo(packageName: String, addresses: Map<String, String>, builder: TreeBuilder = {}) =
        build {
            dir(packageName) {
                builder()
                buildInfoYaml(addresses)
            }
        }

    fun buildInfoYaml(@Language("yaml") code: String = "") = file("BuildInfo.yaml", code)
    fun buildInfoYaml(addresses: Map<String, String>) = buildInfoYaml(
        """
compiled_package_info:
  address_alias_instantiation:
${addresses.map { "    ${it.key}: \"${it.value}\"" }.joinToString("\n")}   
  source_digest: 223A8C78902F806DE810C95988FDDD64D1418DA960621361AD5235D6E5AC654C
  build_flags:
    dev_mode: false
    test_mode: false
    generate_docs: true
    generate_abis: true
    install_dir: ~
    force_recompilation: false
    additional_named_addresses: {}
    architecture: ~
dependencies: []
    """
    )

    fun sources(builder: FileTreeBuilder.() -> Unit) = dir("sources", builder)
    fun dependencies(builder: FileTreeBuilder.() -> Unit) = sources { dir("dependencies", builder) }
    fun build(builder: FileTreeBuilder.() -> Unit) = dir("build", builder)
    fun tests(builder: FileTreeBuilder.() -> Unit) = dir("tests", builder)

    fun _aptos(builder: FileTreeBuilder.() -> Unit) = dir(".aptos", builder)
    fun config_yaml(@Language("YAML") code: String) = file("config.yaml", code)

    fun _aptos_config_yaml(@Language("yaml") code: String) =
        _aptos {
            config_yaml(code)
        }
    fun _aptos_config_yaml_with_profiles(profiles: List<String>) {
        val profilesYaml = profiles.map { """
    $it:
        private_key: "0x4543a4d8eb859b4054b8508aaaa6edb0e9327336e53a8f0134133c4bac2a1354"
        public_key: "0x58af52ff0fbe1e4dd8eb7024b9ef713c68f91d565138b024d035771970dcf97e"
        account: 7f906a4591cfdddcc2c1efb06835ef3faa1feab27d799c24156d5462926fc415
        rest_url: "https://fullnode.testnet.aptoslabs.com"
        """ }
        _aptos {
            config_yaml("""---
profiles:
${profilesYaml.joinToString("\n")}
    """)
        }
    }
}

class FileTree(val rootDirInfo: FilesystemEntry.Directory) {
    fun toTestProject(project: Project, directory: VirtualFile): TestProject {
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

                        val filePath = pathComponents.joinToString(separator = "/")
                        if (hasCaretMarker(fsEntry.text) || "//^" in fsEntry.text || "#^" in fsEntry.text) {
                            filesWithCaret += filePath
                        }
                        if ("//X" in fsEntry.text || "#X" in fsEntry.text) {
                            filesWithNamedElement += filePath
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

class TestProject(
    private val project: Project,
    val rootDirectory: VirtualFile,
    private val filesWithCaret: List<String>,
    private val filesWithNamedElement: List<String>
) {

    val fileWithCaret: String get() = filesWithCaret.singleOrNull() ?: error("No file with caret")
    val fileWithNamedElement: String get() = filesWithNamedElement.singleOrNull() ?: error("No file with named element")

    inline fun <reified T : PsiElement> findElementInFile(path: String): T {
        val element = doFindElementInFile(path)
        return element.ancestorStrict()
            ?: error("No parent of type ${T::class.java} for ${element.text}")
    }

    inline fun <reified T : MvReferenceElement> checkReferenceIsResolved(
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
        val vFile = rootDirectory.findFileByRelativePath(path)
            ?: error("No `$path` file in test project")
        val file = vFile.toPsiFile(project)!!
        return findElementInFile(file, "^")
    }

    fun psiFile(path: String): PsiFileSystemItem {
        val vFile = rootDirectory.findFileByRelativePath(path)
            ?: error("Can't find `$path`")
        val psiManager = PsiManager.getInstance(project)
        return if (vFile.isDirectory) psiManager.findDirectory(vFile)!! else psiManager.findFile(vFile)!!
    }

    fun file(path: String): VirtualFile {
        return rootDirectory.findFileByRelativePath(path) ?: error("Can't find `$path`")
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
