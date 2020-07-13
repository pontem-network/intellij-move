package org.move.lang;

import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.move.lang.MoveTypes.*;

%%

%public
%class _MoveLexer
%implements com.intellij.lexer.FlexLexer
%function advance
%type IElementType
%unicode

EOL=\R
WHITE_SPACE=\s+

NUMBER=0|[1-9][0-9]*
IDENTIFIER=[a-zA-Z][a-zA-Z0-9]*
LIBRA_ADDRESS=0x[1-9a-f]{1,32}

%%
<YYINITIAL> {
  {WHITE_SPACE}        { return WHITE_SPACE; }

  "script"             { return SCRIPT; }
  "address"            { return ADDRESS; }
  "module"             { return MODULE; }
  "public"             { return PUBLIC; }
  "fun"                { return FUN; }
  "resource"           { return RESOURCE; }
  "struct"             { return STRUCT; }
  "loop"               { return LOOP; }
  "if"                 { return IF; }
  "else"               { return ELSE; }
  "let"                { return LET; }
  "mut"                { return MUT; }
  "continue"           { return CONTINUE; }
  "break"              { return BREAK; }
  "return"             { return RETURN; }
  "abort"              { return ABORT; }

  {NUMBER}             { return NUMBER; }
  {IDENTIFIER}         { return IDENTIFIER; }
  {LIBRA_ADDRESS}      { return LIBRA_ADDRESS; }

}

[^] { return BAD_CHARACTER; }
