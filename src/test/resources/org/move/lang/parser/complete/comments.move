// line comment
script {
/* block comment */
/**
multiline comment
*/
    /**
 * One can have /* nested */
    * // block comments
    */
    fun main() {}
// /* unclosed block comment inside line comment
// /* block comment inside line comment */
}

/// doc comment
/// another doc comment
module 0x1::M {
    /// function doc comment
    fun m() {}
    /// doc comment with outer attribute
    #[test_only]
    fun main() {
        let _ = /*caret*/1;
    }

    /// docs
    native fun native_m();

    /// docs
    struct S1 {}
    /// docs
    struct S2(u8);

    /// docs
    enum S {
        /// docs
        One,
        /// docs
        Two
    }
}
