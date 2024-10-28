package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveTestCase

class ResolveLoopLabelsTest: ResolveTestCase() {
    fun `test resolve loop expr label for break`() = checkByCode("""
        module 0x1::m {
            fun main() {
                'label: loop {
                 //X
                    break 'label;
                            //^
                           
                }
            }
        }        
    """)

    fun `test resolve loop expr label for continue`() = checkByCode("""
        module 0x1::m {
            fun main() {
                'label: loop {
                 //X
                    continue 'label;
                              //^
                           
                }
            }
        }        
    """)

    fun `test resolve while expr label for break`() = checkByCode("""
        module 0x1::m {
            fun main() {
                'label: while (true) {
                 //X
                    break 'label;
                            //^
                           
                }
            }
        }        
    """)

    fun `test resolve while expr label for continue`() = checkByCode("""
        module 0x1::m {
            fun main() {
                'label: while (true) {
                 //X
                    continue 'label;
                              //^
                           
                }
            }
        }        
    """)
}