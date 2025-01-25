package org.move.toml

import org.move.utils.tests.annotation.InspectionProjectTestBase
import org.toml.ide.inspections.TomlUnresolvedReferenceInspection

class MoveTomlUnresolvedPathInspectionTest:
    InspectionProjectTestBase(TomlUnresolvedReferenceInspection::class) {

    fun `test local dependency found`() = checkByFileTree(code = {
        namedMoveToml("Root", """
            [dependencies]
            local = "./child"
            
            <caret>
        """)
        sources {}
        dir("child") {
            namedMoveToml("Child")
            sources {  }
        }
    })

    fun `test local dependency not found`() = checkByFileTree(code = {
        namedMoveToml("Root", """
            [dependencies]
            local = "./<warning descr="Cannot resolve file 'child'">child</warning>"
            
            <caret>
        """)
        sources {}
    })

    fun `test no error with extra slash at the end`() = checkByFileTree(code = {
        namedMoveToml("Root", """
            [dependencies]
            local = "./child/"
            
            <caret>
        """)
        sources {}
        dir("child") {
            namedMoveToml("Child")
            sources {  }
        }
    })
}