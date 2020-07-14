package org.move.lang;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.move.lang.MoveElementTypes.*;

%%

%{
  public _MoveLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _MoveLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL=\R
WHITE_SPACE=\s+

WHITESPACE=[ \n\t\r\f]
LINE_COMMENT=("//".*\n)|("//".*\Z)
BLOCK_COMMENT="/"\*(.|[ \t\n\x0B\f\r])*\*"/"
LIBRA_ADDRESS=0x[0-9a-fA-F]{1,40}
BOOL_TRUE=true
BOOL_FALSE=false
IDENTIFIER=[_a-zA-Z][_a-zA-Z0-9]*
NUMBER=0|[1-9][0-9]*
HEXSTRING=x\"([A-F0-9a-f]+)\"
BYTESTRING=b\"(.*)\"

%%
<YYINITIAL> {
  {WHITE_SPACE}        { return WHITE_SPACE; }

  "script"             { return SCRIPT; }
  "address"            { return ADDRESS; }
  "module"             { return MODULE; }
  "public"             { return PUBLIC; }
  "fun"                { return FUN; }
  "acquires"           { return ACQUIRES; }
  "resource"           { return RESOURCE; }
  "struct"             { return STRUCT; }
  "use"                { return USE; }
  "as"                 { return AS; }
  "loop"               { return LOOP; }
  "if"                 { return IF; }
  "else"               { return ELSE; }
  "let"                { return LET; }
  "mut"                { return MUT; }
  "continue"           { return CONTINUE; }
  "break"              { return BREAK; }
  "return"             { return RETURN; }
  "abort"              { return ABORT; }

  {WHITESPACE}         { return WHITESPACE; }
  {LINE_COMMENT}       { return LINE_COMMENT; }
  {BLOCK_COMMENT}      { return BLOCK_COMMENT; }
  {LIBRA_ADDRESS}      { return LIBRA_ADDRESS; }
  {BOOL_TRUE}          { return BOOL_TRUE; }
  {BOOL_FALSE}         { return BOOL_FALSE; }
  {IDENTIFIER}         { return IDENTIFIER; }
  {NUMBER}             { return NUMBER; }
  {HEXSTRING}          { return HEXSTRING; }
  {BYTESTRING}         { return BYTESTRING; }

}

[^] { return BAD_CHARACTER; }
