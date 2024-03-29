package org.move.ui

import com.intellij.remoterobot.RemoteRobot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.move.ui.fixtures.DialogFixture
import org.move.ui.fixtures.ideaFrame
import org.move.ui.utils.RemoteRobotExtension

@ExtendWith(RemoteRobotExtension::class)
class IdeaFrameTest {

    @Test
    fun `run view function test`(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideaFrame {
            val editorGutter = textEditor().gutter
            editorGutter.getIcons().first().click()

        }

        val paramsDialog = DialogFixture.byTitle("Edit Function Parameters")
    }
}