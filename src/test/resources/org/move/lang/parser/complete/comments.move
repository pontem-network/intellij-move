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
module M {
    /// function doc comment
    fun m() {}
    /// doc comment with outer attribute
    #[test_only]
    fun main() {}
}
