package org.move.ide.inspections.imports

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import org.intellij.lang.annotations.Language
import org.move.ide.inspections.MvUnresolvedReferenceInspection
import org.move.utils.tests.FileTreeBuilder
import org.move.utils.tests.annotation.InspectionProjectTestBase

class AutoImportFixProjectTest : InspectionProjectTestBase(MvUnresolvedReferenceInspection::class) {
    fun `test import method from another file`() = checkAutoImportFixByText(
        {
            moveToml("""""")
            sources {
                move(
                    "Mod.move", """
            module 0x1::Mod {
                public fun call() {}
            }    
            """
                )
                move(
                    "Main.move", """
            module 0x1::Main {
                fun main() {
                    <error descr="Unresolved reference: `call`">/*caret*/call</error>();
                }
            }    
            """
                )
            }
        }, """
            module 0x1::Main {
                use 0x1::Mod::call;

                fun main() {
                    call();
                }
            }    
        """
    )

    fun `test auto import from local dependency`() = checkAutoImportFixByText(
        {
            dir("move-stdlib") {
                moveToml(
                    """
                [package]
                name = "MoveStdlib"    
                """
                )
                sources {
                    move(
                        "Mod.move",
                        """
                        module 0x1::Mod {
                            public fun call() {}
                        }
                        """
                    )

                }
            }
            moveToml(
                """
            [dependencies]
            MoveStdlib = { local = "./move-stdlib" }    
            """
            )
            sources {
                move(
                    "Main.move", """
            module 0x1::Main {
                fun main() {
                    <error descr="Unresolved reference: `call`">/*caret*/call</error>();
                }
            }    
            """
                )
            }
        }, """
            module 0x1::Main {
                use 0x1::Mod::call;

                fun main() {
                    call();
                }
            }    
        """
    )

    fun `test auto import from git dependency`() = checkAutoImportFixByText(
        {
            build {
                dir("MoveStdlib") {
                    buildInfoYaml("""
compiled_package_info:
  package_name: MoveStdlib
  address_alias_instantiation:
    Std: "0000000000000000000000000000000000000000000000000000000000000001"
  module_resolution_metadata:
    ? address: "0000000000000000000000000000000000000000000000000000000000000001"
      name: Signer
    : Std
  source_digest: 7DD3C823198A0DF84D836ED674128977405E6CD644037DC421F468B962231A94
  build_flags:
    dev_mode: false
    test_mode: false
    generate_docs: false
    generate_abis: false
    install_dir: ~
    force_recompilation: false
    additional_named_addresses: {}
    language_flavor: ~
dependencies: []
""")
                    sources {
                        move(
                            "Mod.move",
                            """
                        module 0x1::Mod {
                            public fun call() {}
                        }
                        """
                        )
                    }
                }
            }
            moveToml(
                """
            [dependencies.MoveStdlib]
            git = "https://github.com/aptos-labs/hello.git"
            rev = "012f07809431e936d32a9be8620089ad1834d4e9"
            """
            )
            sources {
                move(
                    "Main.move", """
            module 0x1::Main {
                fun main() {
                    <error descr="Unresolved reference: `call`">/*caret*/call</error>();
                }
            }    
            """
                )
            }
        }, """
            module 0x1::Main {
                use 0x1::Mod::call;

                fun main() {
                    call();
                }
            }    
        """
    )

    private fun checkAutoImportFixByText(
        before: FileTreeBuilder.() -> Unit,
        @Language("Move") after: String,
    ) = doTest { checkFixByFileTree(AutoImportFix.NAME, before, after) }

    private inline fun doTest(action: () -> Unit) {
        val fileIndex = FileBasedIndex.getInstance()
        fileIndex.ensureUpToDate(MvNamedElementIndex.KEY, project, GlobalSearchScope.allScope(project))

        val inspection = inspection as MvUnresolvedReferenceInspection
        val defaultValue = inspection.ignoreWithoutQuickFix
        try {
            inspection.ignoreWithoutQuickFix = false
            action()
        } finally {
            inspection.ignoreWithoutQuickFix = defaultValue
        }
    }
}
