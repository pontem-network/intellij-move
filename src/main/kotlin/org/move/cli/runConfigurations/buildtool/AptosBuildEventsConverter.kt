package org.move.cli.runConfigurations.buildtool

import com.intellij.build.events.BuildEvent
import com.intellij.build.output.BuildOutputInstantReader
import com.intellij.build.output.BuildOutputParser
import java.util.function.Consumer

class AptosBuildEventsConverter(private val context: AptosBuildContextBase): BuildOutputParser {
    override fun parse(
        line: String,
        reader: BuildOutputInstantReader,
        messageConsumer: Consumer<in BuildEvent>
    ): Boolean {

        return true

    }
}
