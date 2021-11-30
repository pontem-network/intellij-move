package org.move.ide

import com.intellij.lang.Commenter

class MoveCommenter: Commenter {
    override fun getLineCommentPrefix() = "//"

    override fun getBlockCommentPrefix() = "/*"
    override fun getBlockCommentSuffix() = "*/"

    override fun getCommentedBlockCommentPrefix() = "*//*"
    override fun getCommentedBlockCommentSuffix() = "*//*"
}
