package org.move.cli.filters

import com.intellij.util.PathUtil
import org.move.cli.MoveFileHyperlinkFilter
import org.move.utils.tests.HighlightFilterTestBase
import java.nio.file.Paths
import kotlin.io.path.Path

class MoveFileHyperlinkFilterTest: HighlightFilterTestBase() {
    fun `test compilation failure hyperlink`() {
        val rootDir = projectDir.toNioPath()
        checkHighlights(
            MoveFileHyperlinkFilter(project, rootDir),
            {
                moveToml("""
                    [package]
                    name = "MyPackage"
                    
                    [addresses]
                    main = "0x1234"
                """)
                sources {
                    move("main.move", """
                        module main::main {
                            fun main() {/*caret*/}
                        }                    
                    """)
                }
            },
            """
        ┌─ ${Paths.get(rootDir.toString(), "sources", "main.move")}:1:1
            """.trimIndent(),
            """
        ┌─ [${Paths.get(rootDir.toString(), "sources", "main.move")}:1:1 -> main.move]
            """.trimIndent(),
        )
    }
}
