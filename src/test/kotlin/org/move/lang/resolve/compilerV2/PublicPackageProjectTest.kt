package org.move.lang.resolve.compilerV2

import org.move.ide.inspections.fixes.CompilerV2Feat.PUBLIC_PACKAGE
import org.move.utils.tests.CompilerV2Features
import org.move.utils.tests.resolve.ResolveProjectTestCase

@CompilerV2Features(PUBLIC_PACKAGE)
class PublicPackageProjectTest: ResolveProjectTestCase() {
    @CompilerV2Features()
    fun `test package function is not available from another module in compiler v1`() =
        checkByFileTree {
            namedMoveToml("MyPackage")
            sources {
                move(
                    "a.move", """
        module 0x1::a {
            public(package) fun call() {}
        }        
            """
                )
                main(
                    """
        module 0x1::main {
            use 0x1::a::call;
            fun main() {
                call();
                //^ unresolved
            }
        }
            """
                )
            }
        }

    @CompilerV2Features()
    fun `test package function is not available from another module in compiler v1 for module import`() =
        checkByFileTree {
            namedMoveToml("MyPackage")
            sources {
                move(
                    "a.move", """
        module 0x1::a {
            public(package) fun call() {}
        }        
            """
                )
                main(
                    """
        module 0x1::main {
            use 0x1::a;
            fun main() {
                a::call();
                  //^ unresolved
            }
        }
            """
                )
            }
        }

    fun `test package function is available from another module of the same package`() =
        checkByFileTree {
            namedMoveToml("MyPackage")
            sources {
                move(
                    "a.move", """
        module 0x1::a {
            public(package) fun call() {}
                               //X
        }        
            """
                )
                main(
                    """
        module 0x1::main {
            use 0x1::a::call;
            fun main() {
                call();
                //^
            }
        }
            """
                )
            }
        }

    fun `test package function is available from another module of the same package with module import`() =
        checkByFileTree {
            namedMoveToml("MyPackage")
            sources {
                move(
                    "a.move", """
        module 0x1::a {
            public(package) fun call() {}
                               //X
        }        
            """
                )
                main(
                    """
        module 0x1::main {
            use 0x1::a;
            fun main() {
                a::call();
                  //^
            }
        }
            """
                )
            }
        }

    fun `test package function is not available from another package`() =
        checkByFileTree {
            dir("package2") {
                namedMoveToml("MyPackage2")
                sources {
                    move(
                        "a.move", """
        module 0x1::a {
            public(package) fun call() {}
        }        
            """
                    )
                }
            }
            moveToml("""
            [package]
            name = "MyPackage"
            
            [dependencies]
            MyPackage2 = { local = "./package2" }
            """)
            sources {
                main(
                    """
        module 0x1::main {
            use 0x1::a::call;
            fun main() {
                call();
                //^ unresolved
            }
        }
            """
                )
            }
        }

    fun `test package function is not available from another package with module import`() =
        checkByFileTree {
            dir("package2") {
                namedMoveToml("MyPackage2")
                sources {
                    move(
                        "a.move", """
        module 0x1::a {
            public(package) fun call() {}
        }        
            """
                    )
                }
            }
            moveToml("""
            [package]
            name = "MyPackage"
            
            [dependencies]
            MyPackage2 = { local = "./package2" }
            """)
            sources {
                main(
                    """
        module 0x1::main {
            use 0x1::a;
            fun main() {
                a::call();
                   //^ unresolved
            }
        }
            """
                )
            }
        }
}