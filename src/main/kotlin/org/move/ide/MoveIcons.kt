package org.move.ide

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object MoveIcons {
    val MOVE = load("/icons/move.svg")
    val APTOS = load("/icons/aptos.svg")

    val ADDRESS = load("/icons/annotationtype.png")
    val MODULE = load("/icons/module.svg")

    val STRUCT = load("/icons/struct.svg")
    val STRUCT_FIELD = AllIcons.Nodes.Field
    val SCHEMA = AllIcons.Nodes.Static

    val CONST = AllIcons.Nodes.Constant
    val FUNCTION = AllIcons.Nodes.Function

    val BINDING = AllIcons.Nodes.Variable

    val VARIABLE = AllIcons.Nodes.Variable
    val PARAMETER = AllIcons.Nodes.Parameter

    val PUBLISH = AllIcons.Actions.Upload

    val RUN_TEST_ITEM = AllIcons.RunConfigurations.TestState.Run
    val RUN_ALL_TESTS_IN_ITEM = AllIcons.RunConfigurations.TestState.Run_run

    val RUN_TRANSACTION_ITEM = AllIcons.Nodes.RunnableMark
    val VIEW_FUNCTION_ITEM = AllIcons.Actions.Preview
    val TEST_GREEN = AllIcons.RunConfigurations.TestState.Green2
    val TEST_RED = AllIcons.RunConfigurations.TestState.Red2

    val ITEM_SPECS_GUTTER = load("/icons/down.svg")
    val ITEM_SPECS_GUTTER_DARK = load("/icons/down_dark.svg")

    val SPEC_SOURCE_MODULE_GUTTER = load("/icons/up.svg")
    val SPEC_SOURCE_MODULE_GUTTER_DARK = load("/icons/up_dark.svg")

    private fun load(path: String): Icon = IconLoader.getIcon(path, MoveIcons::class.java)
}
