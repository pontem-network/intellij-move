package org.move.ide.annotator.syntaxErrors.compilerV2

import org.move.ide.annotator.MvSyntaxErrorAnnotator
import org.move.ide.inspections.fixes.CompilerV2Feat.PUBLIC_PACKAGE
import org.move.utils.tests.CompilerV2Features
import org.move.utils.tests.annotation.AnnotatorTestCase

class PublicPackageTest: AnnotatorTestCase(MvSyntaxErrorAnnotator::class) {
    @CompilerV2Features(PUBLIC_PACKAGE)
    fun `test no error with compiler v2 public package`() = checkWarnings("""
        module 0x1::m {
            public(package) fun call() {}
        }        
    """)

    @CompilerV2Features(PUBLIC_PACKAGE)
    fun `test no error with compiler v2 public friend`() = checkWarnings("""
        module 0x1::m {
            public(friend) fun call() {}
        }        
    """)

    @CompilerV2Features(PUBLIC_PACKAGE)
    fun `test cannot use public package together with public friend`() = checkWarnings("""
        module 0x1::m {
            <error descr="public(package) and public(friend) cannot be used together in the same module">public(friend)</error> fun call1() {}
            <error descr="public(package) and public(friend) cannot be used together in the same module">public(package)</error> fun call2() {}
                         
        }        
    """)

    @CompilerV2Features()
    fun `test cannot use public package in compiler v1`() = checkWarnings("""
        module 0x1::m {
            <error descr="public(package) is not supported in Aptos Move V1">public(package)</error> fun call() {}
                         
        }        
    """)
}