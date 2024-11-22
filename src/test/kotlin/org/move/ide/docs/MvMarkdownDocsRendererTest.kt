package org.move.ide.docs

import org.move.utils.tests.MvDocumentationProviderTestCase

class MvMarkdownDocsRendererTest: MvDocumentationProviderTestCase() {

    fun `test markdown text styles`() = doTest("""
/// This string contains some *bold* and **italic** words.
module 0x1::M {}
          //^   
    """, """
<div class='definition'><pre>module 0x1::M</pre></div>
<div class='content'><p>This string contains some <em>bold</em> and <strong>italic</strong> words.</p></div>
    """)

    fun `test markdown inline code`() = doTest("""
/// Maybe some `inline` keyword
module 0x1::M {}
          //^   
    """, """
<div class='definition'><pre>module 0x1::M</pre></div>
<div class='content'><p>Maybe some <code>inline</code> keyword</p></div>
    """)

    fun `test markdown multiline code`() = doTest("""
/// Move code:
/// ```
/// module 0x1::M {}
/// ```
module 0x1::M {}
          //^   
    """, """
<div class='definition'><pre>module 0x1::M</pre></div>
<div class='content'><p>Move code:</p><pre style="text-indent: 10px; margin-bottom: -20px;"><span style="color: #000080; font-weight: bold;">module </span><span style="color: #000000;">0x1::M {}</span>
</pre>
</div>
    """)

    fun `test markdown multiline code with extra spaces`() = doTest("""
/// Move code:
/// ```
/// module 0x1::M {
///    // comment
/// }
/// ```
module 0x1::M {}
          //^   
    """, """
<div class='definition'><pre>module 0x1::M</pre></div>
<div class='content'><p>Move code:</p><pre style="text-indent: 10px; margin-bottom: -20px;"><span style="color: #000080; font-weight: bold;">module </span><span style="color: #000000;">0x1::M {</span>
   <span style="color: #808080; font-style: italic;">// comment</span>
<span style="color: #000000;">}</span>
</pre>
</div>
    """)

    fun `test markdown list`() = doTest("""
/// - The number of "items" in global storage.
/// - The number of bytes in global storage.
module 0x1::M {}
          //^   
    """, """
<div class='definition'><pre>module 0x1::M</pre></div>
<div class='content'><ul><li>The number of &quot;items&quot; in global storage.</li><li>The number of bytes in global storage.</li></ul></div>
    """)

    fun `test markdown numbered list`() = doTest("""
/// 1. The number of "items" in global storage.
/// 2. The number of bytes in global storage.
module 0x1::M {}
          //^   
    """, """
<div class='definition'><pre>module 0x1::M</pre></div>
<div class='content'><ol><li>The number of &quot;items&quot; in global storage.</li><li>The number of bytes in global storage.</li></ol></div>
    """)
}