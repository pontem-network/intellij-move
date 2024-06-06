package org.move.ide

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.LayeredIcon
import com.intellij.ui.LayeredIcon.Companion.layeredIcon
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.Icon

object MoveIcons {
    val MOVE_LOGO = load("/icons/move_logo.svg")
    val APTOS_LOGO = load("/icons/aptos.svg")
    val SUI_LOGO = load("/icons/sui.svg")

    val MV_LOGO = load("/icons/move_logo.svg")

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

    val FINAL_MARK = AllIcons.Nodes.FinalMark
    val STATIC_MARK = AllIcons.Nodes.StaticMark
    val TEST_MARK = AllIcons.Nodes.JunitTestMark

    val GEAR = load("/icons/gear.svg")
    val GEAR_OFF = load("/icons/gearOff.svg")
    val GEAR_ANIMATED = AnimatedIcon(
        AnimatedIcon.Default.DELAY,
        GEAR,
        GEAR.rotated(15.0),
        GEAR.rotated(30.0),
        GEAR.rotated(45.0)
    )

    private fun load(path: String): Icon = IconLoader.getIcon(path, MoveIcons::class.java)
}


fun Icon.addFinalMark(): Icon = layeredIcon(arrayOf(this, MoveIcons.FINAL_MARK))

fun Icon.addStaticMark(): Icon = layeredIcon(arrayOf(this, MoveIcons.STATIC_MARK))

fun Icon.addTestMark(): Icon = layeredIcon(arrayOf(this, MoveIcons.TEST_MARK))

fun Icon.multiple(): Icon {
    val compoundIcon = LayeredIcon(2)
    compoundIcon.setIcon(this, 0, 2 * iconWidth / 5, 0)
    compoundIcon.setIcon(this, 1, 0, 0)
    return compoundIcon
}

/**
 * Rotates the icon by the given angle, in degrees.
 *
 * **Important**: Do ***not*** rotate the icon by ±90 degrees (or any sufficiently close amount)!
 * The implementation of rotation by that amount in AWT is broken, and results in erratic shifts for composed
 * transformations. In other words, the (final) transformation matrix as a function of rotation angle
 * is discontinuous at those points.
 */
fun Icon.rotated(angle: Double): Icon {
    val q = this
    return object: Icon by this {
        override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
            val g2d = g.create() as Graphics2D
            try {
                g2d.translate(x.toDouble(), y.toDouble())
                g2d.rotate(Math.toRadians(angle), iconWidth / 2.0, iconHeight / 2.0)
                q.paintIcon(c, g2d, 0, 0)
            } finally {
                g2d.dispose()
            }
        }
    }
}
