package org.move.lang;

import com.intellij.psi.tree.IElementType;
import com.intellij.lexer.FlexLexer;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.move.lang.MvElementTypes.*;
import static org.move.lang.MoveParserDefinition.*;

%%

%{
    /**
        * Dedicated storage for starting position of some previously successful
        * match
    */
    private int zzPostponedMarkedPos = -1;

    /**
        * Dedicated nested-comment level counter
    */
    private int zzNestedCommentLevel = 0;
%}

%{
    public _MoveLexer() {
        this((java.io.Reader)null);
    }

    IElementType imbueBlockComment() {
        assert(zzNestedCommentLevel == 0);
        yybegin(YYINITIAL);

        zzStartRead = zzPostponedMarkedPos;
        zzPostponedMarkedPos = -1;

        return BLOCK_COMMENT;
    }
%}

%public
%class _MoveLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

%s IN_BLOCK_COMMENT

///////////////////////////////////////////////////////////////////////////////////////////////////
// Whitespaces
///////////////////////////////////////////////////////////////////////////////////////////////////
EOL_WS           = \n | \r | \r\n
LINE_WS          = [\ \t]
WHITE_SPACE_CHAR = {EOL_WS} | {LINE_WS}
WHITE_SPACE      = {WHITE_SPACE_CHAR}+

///////////////////////////////////////////////////////////////////////////////////////////////////
// Comments
///////////////////////////////////////////////////////////////////////////////////////////////////
//EOL_DOC_LINE  = {LINE_WS}*("///".*)
EOL_DOC_COMMENT =  ("///".*)
//OUTER_EOL_DOC = ({EOL_DOC_LINE}{EOL_WS})*{EOL_DOC_LINE}

///////////////////////////////////////////////////////////////////////////////////////////////////
// Literals
///////////////////////////////////////////////////////////////////////////////////////////////////
PLACEHOLDER_ADDRESS=\{\{[_a-zA-Z][_a-zA-Z0-9]*\}\}
DIEM_ADDRESS=0x[0-9a-fA-F]{1,64}
BECH32_ADDRESS=wallet1[A-Z0-9a-z&&[^boi1]]{6,83}
POLKADOT_ADDRESS=[1-9A-HJ-NP-Za-km-z]{40}[1-9A-HJ-NP-Za-km-z]*

BOOL_LITERAL=(true)|(false)
INTEGER_LITERAL=[0-9]+[a-zA-Z0-9]*
HEX_INTEGER_LITERAL=0x[0-9a-fA-F]+[a-zA-Z0-9]*
HEX_STRING_LITERAL=x\" ( [A-F0-9a-f]* ) (\")?
BYTE_STRING_LITERAL=b\" ( [^\\\"\n] | \\[^] )* (\")?
//BYTE_STRING_LITERAL=b\" ( [^\"\n] | (\\\")] )* (\")?

IDENTIFIER=[_a-zA-Z][_a-zA-Z0-9]*

%%
<YYINITIAL> {
      {WHITE_SPACE}        { return WHITE_SPACE; }
      {EOL_DOC_COMMENT}                 { return EOL_DOC_COMMENT; }
      "//" .*              { return EOL_COMMENT; }
      "/*"                 {
          yybegin(IN_BLOCK_COMMENT); yypushback(2);
       }
}

<YYINITIAL> {
      // operators
      "{"        { return L_BRACE; }
      "}"        { return R_BRACE; }

      "["        { return L_BRACK; }
      "]"        { return R_BRACK; }
      "("        { return L_PAREN; }
      ")"        { return R_PAREN; }
      "::"       { return COLON_COLON; }
      ":"        { return COLON; }
      ";"        { return SEMICOLON; }
      ","        { return COMMA; }
      "."        { return DOT; }
      "="        { return EQ; }
      "=="       { return EQ_EQ; }
      "!="       { return NOT_EQ; }

      "!"        { return EXCL; }
      "+"        { return PLUS; }
      "-"        { return MINUS; }
      "*"        { return MUL; }
      "/"        { return DIV; }
      "%"        { return MODULO; }
      "^"        { return XOR; }

      "<"        { return LT; }
      ">"        { return GT; }
      "&"        { return AND; }
      "|"        { return OR; }
      "@"        { return AT; }
      "#"        { return HASH; }

      // keywords
      "script"         { return SCRIPT_KW; }
      "module"         { return MODULE_KW; }
      "const"          { return CONST_KW; }
      "native"         { return NATIVE; }
      "public"         { return PUBLIC; }
      "fun"            { return FUN; }
      "acquires"       { return ACQUIRES; }
      "struct"         { return STRUCT_KW; }
      "use"            { return USE; }
      "as"             { return AS; }
      "mut"            { return MUT; }
      "copy"           { return COPY; }
      "move"           { return MOVE; }
      "return"         { return RETURN; }
      "abort"          { return ABORT; }
      "break"          { return BREAK; }
      "continue"       { return CONTINUE; }
      "if"             { return IF; }
      "else"           { return ELSE; }
      "loop"           { return LOOP; }
      "while"          { return WHILE; }
      "let"            { return LET; }
      "phantom"            { return PHANTOM; }
      "spec"           { return SPEC; }

  {DIEM_ADDRESS}          { return DIEM_ADDRESS; }
  {PLACEHOLDER_ADDRESS}          { return PLACEHOLDER_ADDRESS; }
  {BECH32_ADDRESS}          { return BECH32_ADDRESS; }
  {POLKADOT_ADDRESS}          { return POLKADOT_ADDRESS; }

  {BOOL_LITERAL}             { return BOOL_LITERAL; }
  {HEX_INTEGER_LITERAL}          { return HEX_INTEGER_LITERAL; }
  {INTEGER_LITERAL}          { return INTEGER_LITERAL; }
  {HEX_STRING_LITERAL}       { return HEX_STRING_LITERAL; }
  {BYTE_STRING_LITERAL}      { return BYTE_STRING_LITERAL; }
  {IDENTIFIER}               { return IDENTIFIER; }
}

<IN_BLOCK_COMMENT> {
  "/*"    {
          if (zzNestedCommentLevel++ == 0)
              zzPostponedMarkedPos = zzStartRead;
      }
  "*/"    {
          if (--zzNestedCommentLevel == 0)
              return imbueBlockComment();
      }
  <<EOF>> {
          zzNestedCommentLevel = 0; return imbueBlockComment();
 }
  [^]     { }
}

[^] { return BAD_CHARACTER; }
