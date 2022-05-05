package org.move.ide

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object MoveIcons {
    val MOVE = load("/icons/move.svg")
    val MOVE_BYTECODE = load("/icons/mv.svg")

    val ADDRESS = AllIcons.Nodes.Annotationtype
    val MODULE = AllIcons.Nodes.Method

    val STRUCT = AllIcons.Nodes.AbstractClass
    val STRUCT_FIELD = AllIcons.Nodes.Field
    val SCHEMA = AllIcons.Nodes.Static

    val CONST = AllIcons.Nodes.Constant
    val FUNCTION = AllIcons.Nodes.Function

    val BINDING = AllIcons.Nodes.Variable

    val VARIABLE = AllIcons.Nodes.Variable
    val PARAMETER = AllIcons.Nodes.Parameter

    val PUBLISH = AllIcons.Actions.Upload
    val TEST = AllIcons.RunConfigurations.TestState.Run
    val TEST_GREEN = AllIcons.RunConfigurations.TestState.Green2
    val TEST_RED = AllIcons.RunConfigurations.TestState.Red2

    private fun load(path: String): Icon = IconLoader.getIcon(path, MoveIcons::class.java)
}
