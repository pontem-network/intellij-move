package org.move.cli.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.CachingSimpleNode
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.ui.treeStructure.SimpleTreeStructure
import org.move.cli.MovePackage
import org.move.cli.MoveProject
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.entryFunctions
import org.move.lang.core.psi.ext.hasTestAttr
import org.move.lang.core.psi.ext.hasTestOnlyAttr
import org.move.lang.core.psi.ext.viewFunctions
import org.move.lang.core.types.fqName
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
        this.root = MoveSimpleNode.Root(moveProjects)
        treeModel.invalidateAsync()
    }

    sealed class MoveSimpleNode(parent: SimpleNode?) : CachingSimpleNode(parent) {
        abstract fun toTestString(): String

        class Root(private val moveProjects: List<MoveProject>) : MoveSimpleNode(null) {
            override fun buildChildren(): Array<SimpleNode> =
                moveProjects.map { TreeProject(it, this) }.sortedBy { it.name }.toTypedArray()

            override fun getName() = ""
            override fun toTestString() = "Root"
        }

        open class Package(val movePackage: MovePackage, parent: SimpleNode) : MoveSimpleNode(parent) {
            init {
                icon = MoveIcons.MOVE_LOGO
            }

            override fun buildChildren(): Array<SimpleNode> {
                val modules = mutableListOf<MvModule>()
                val scriptFunctions = mutableListOf<MvFunction>()
                val viewFunctions = mutableListOf<MvFunction>()
                for (folder in movePackage.moveFolders()) {
                    folder.iterateMoveFiles(movePackage.project) {
                        for (module in it.modules()) {
                            if (!module.hasTestOnlyAttr) modules.add(module)
                            scriptFunctions.addAll(
                                module.entryFunctions()
                                    .filter { fn -> !fn.hasTestOnlyAttr && !fn.hasTestAttr })
                            viewFunctions.addAll(
                                module.viewFunctions()
                                    .filter { fn -> !fn.hasTestOnlyAttr && !fn.hasTestAttr })
                        }
                        true
                    }
                }
                return listOfNotNull(
                    modules.takeIf { it.isNotEmpty() }?.let { Modules(it, this) },
                    scriptFunctions.takeIf { it.isNotEmpty() }?.let { Entrypoints(it, this) },
                    viewFunctions.takeIf { it.isNotEmpty() }?.let { Views(it, this) },
                ).toTypedArray()
            }

            override fun getName(): String = movePackage.packageName
            override fun toTestString(): String = "Package($name)"
        }

        class TreeProject(val moveProject: MoveProject, parent: SimpleNode) :
            Package(moveProject.currentPackage, parent) {

            override fun buildChildren(): Array<SimpleNode> {
                val dependencyPackages = moveProject.dependencies.map { it.package_ }
                return arrayOf(
                    *super.buildChildren(),
                    DependencyPackages(dependencyPackages, this),
                )
            }

            override fun getName(): String = moveProject.currentPackage.packageName
            override fun toTestString(): String = "Project($name)"
        }

        class DependencyPackages(val packages: List<MovePackage>, parent: SimpleNode) : MoveSimpleNode(parent) {
            override fun buildChildren(): Array<SimpleNode> {
                return packages.map { Package(it, this) }.toTypedArray()
            }

            override fun getName(): String = "Dependencies"
            override fun toTestString(): String = "Dependencies"
        }

        class Modules(val modules: List<MvModule>, parent: SimpleNode) : MoveSimpleNode(parent) {
            override fun buildChildren(): Array<SimpleNode> =
                modules.map { Module(it, this) }.toTypedArray()

            override fun getName(): String = "Modules"
            override fun toTestString(): String = "Modules"
        }

        class Module(val module: MvModule, parent: SimpleNode) : MoveSimpleNode(parent) {
            init {
                icon = MoveIcons.MODULE
            }

            override fun buildChildren(): Array<SimpleNode> = emptyArray()
            override fun getName(): String = runReadAction { module.fqName()?.editorText() ?: "null" }
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
            init {
                icon = MoveIcons.FUNCTION
            }

            override fun buildChildren(): Array<SimpleNode> = emptyArray()
            override fun getName(): String = runReadAction { function.fqName()?.editorText() ?: "null" }
            override fun toTestString(): String = "Entrypoint($name)"
        }

        class Views(val functions: List<MvFunction>, parent: SimpleNode) : MoveSimpleNode(parent) {
            override fun buildChildren(): Array<SimpleNode> {
                return functions.map { View(it, this) }.toTypedArray()
            }

            override fun getName(): String = "Views"
            override fun toTestString(): String = "Views"
        }

        class View(val function: MvFunction, parent: SimpleNode) : MoveSimpleNode(parent) {
            init {
                icon = MoveIcons.FUNCTION
            }

            override fun buildChildren(): Array<SimpleNode> = emptyArray()
            override fun getName(): String = runReadAction { function.fqName()?.editorText() ?: "null" }
            override fun toTestString(): String = "View($name)"
        }
    }
}
