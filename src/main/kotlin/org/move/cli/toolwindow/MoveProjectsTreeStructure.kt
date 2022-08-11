package org.move.cli.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.CachingSimpleNode
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.ui.treeStructure.SimpleTreeStructure
import org.move.cli.MoveProject
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.modules
import org.move.stdext.iterateMoveFiles

class MoveProjectsTreeStructure(
    tree: MoveProjectsTree,
    parentDisposable: Disposable,
    private var moveProjects: List<MoveProject> = emptyList()
) : SimpleTreeStructure() {

    private val treeModel = StructureTreeModel(this, parentDisposable)
    private var root = MoveSimpleNode.Root(moveProjects)

    init {
        tree.model = AsyncTreeModel(treeModel, parentDisposable)
    }

    override fun getRootElement() = root

    fun updateMoveProjects(moveProjects: List<MoveProject>) {
        this.moveProjects = moveProjects
        root = MoveSimpleNode.Root(moveProjects)
        treeModel.invalidate()
    }

    sealed class MoveSimpleNode(parent: SimpleNode?) : CachingSimpleNode(parent) {
        abstract fun toTestString(): String

        class Root(private val moveProjects: List<MoveProject>) : MoveSimpleNode(null) {
            override fun buildChildren(): Array<SimpleNode> =
                moveProjects.map { Project(it, this) }.sortedBy { it.name }.toTypedArray()

            override fun getName() = ""
            override fun toTestString() = "Root"
        }

        class Project(val moveProject: MoveProject, parent: SimpleNode) : MoveSimpleNode(parent) {
            init {
                icon = MoveIcons.MOVE
            }

            override fun buildChildren(): Array<SimpleNode> {
                val modules = mutableListOf<MvModule>()
                val testModules = mutableListOf<MvModule>()
                val scriptFunctions = mutableListOf<MvFunction>()
                for (folder in moveProject.currentPackage.moveFolders()) {
                    folder.iterateMoveFiles(moveProject.project) {
                        for (module in it.modules()) {
                            if (!module.isTestOnly) modules.add(module)
                            if (module.testFunctions().isNotEmpty()) testModules.add(module)
                            scriptFunctions.addAll(
                                module.entryFunctions()
                                    .filter { fn -> !fn.isTestOnly && !fn.isTest })
                        }
                        true
                    }
                }
                return arrayOf(
                    Modules(emptyList(), this),
                    Entrypoints(scriptFunctions, this),
                    Tests(emptyList(), this)
                )
            }

            override fun getName(): String = moveProject.currentPackage.packageName
            override fun toTestString(): String = "Project($name)"
        }

        class Modules(val modules: List<MvModule>, parent: SimpleNode) : MoveSimpleNode(parent) {
            override fun buildChildren(): Array<SimpleNode> =
                modules.map { Module(it, this) }.toTypedArray()

            override fun getName(): String = "Modules"
            override fun toTestString(): String = "Modules"
        }

        class Module(val module: MvModule, parent: SimpleNode) : MoveSimpleNode(parent) {
            override fun buildChildren(): Array<SimpleNode> = emptyArray()
            override fun getName(): String = module.fqName
            override fun toTestString(): String = "Module($name)"
        }

        class Entrypoints(val functions: List<MvFunction>, parent: SimpleNode) : MoveSimpleNode(parent) {
            override fun buildChildren(): Array<SimpleNode> {
                return functions.map { Entrypoint(it, this) }.toTypedArray()
            }

            override fun getName(): String = "Entrypoints"
            override fun toTestString(): String = "Entrypoints"
        }

        class Entrypoint(val function: MvFunction, parent: SimpleNode) : MoveSimpleNode(parent) {
            override fun buildChildren(): Array<SimpleNode> = emptyArray()
            override fun getName(): String = function.fqName
            override fun toTestString(): String = "Entrypoint($name)"
        }

        class Tests(val testModules: List<MvModule>, parent: SimpleNode) : MoveSimpleNode(parent) {
            override fun buildChildren(): Array<SimpleNode> {
                return testModules.map { TestModule(it, this) }.toTypedArray()
            }

            override fun getName(): String = "Tests"
            override fun toTestString(): String = "Tests"
        }

        class TestModule(val module: MvModule, parent: SimpleNode) : MoveSimpleNode(parent) {
            override fun buildChildren(): Array<SimpleNode> {
                return module.testFunctions().map { TestFunction(it, this) }.toTypedArray()
            }

            override fun getName(): String = module.name ?: ""
            override fun toTestString(): String = "TestModule"
        }

        class TestFunction(val function: MvFunction, parent: SimpleNode) : MoveSimpleNode(parent) {
            override fun buildChildren(): Array<SimpleNode> = emptyArray()
            override fun getName(): String = function.name ?: "<unknown>"
            override fun toTestString(): String = "TestFunction"
        }
    }
}
