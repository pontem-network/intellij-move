package org.move.lang;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import java.util.ArrayList;import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.move.lang.MoveElementTypes.*;

%%

%{
//  private int bracesDepth = 0;

  private boolean isSpecDef = false;

  private int specBracesDepth = -1;

  private ArrayList<Integer> stateStack = new ArrayList<Integer>();
%}

%{
  public _MoveLexer() {
    this((java.io.Reader)null);
  }

  public void pushState(int state) {
      this.stateStack.add(yystate());
      yybegin(state);
  }

  public void popState() {
      int oldState = this.stateStack.remove(this.stateStack.size() - 1);
      yybegin(oldState);
  }
%}

%public
%class _MoveLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

%s IN_BLOCK_COMMENT
%s BEGIN_SPEC
%s IN_SPEC
//%s IN_APPLY_TO

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
LINE_COMMENT=("//".*\n)|("//".*\R)

///////////////////////////////////////////////////////////////////////////////////////////////////
// Literals
///////////////////////////////////////////////////////////////////////////////////////////////////
ADDRESS_LITERAL=0x[0-9a-fA-F]{1,40}
BECH32_ADDRESS_LITERAL=wallet1[A-Z0-9a-z&&[^boi1]]{6,83}

BOOL_LITERAL=(true)|(false)
INTEGER_LITERAL=[0-9]+((u8)|(u64)|(u128))?
HEX_STRING_LITERAL=x\"([A-F0-9a-f]*)\"
BYTE_STRING_LITERAL=b\"(.*)\"

IDENTIFIER=[_a-zA-Z][_a-zA-Z0-9]*
//FUNCTION_PATTERN_NAME=[*_a-zA-Z][*_a-zA-Z0-9]*

%%
<YYINITIAL, BEGIN_SPEC, IN_SPEC> {
      {WHITE_SPACE}        { return WHITE_SPACE; }
      {LINE_COMMENT}       { return LINE_COMMENT; }

      "/*"                 { pushState(IN_BLOCK_COMMENT); yypushback(2); }
}

<YYINITIAL, IN_SPEC> {
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

      // keywords
      "address"        { return ADDRESS; }
      "script"         { yybegin(YYINITIAL); return SCRIPT; }
      "module"         { yybegin(YYINITIAL); return MODULE; }
      "const"          { yybegin(YYINITIAL); return CONST; }
      "native"         { yybegin(YYINITIAL); return NATIVE; }
      "public"         { yybegin(YYINITIAL); return PUBLIC; }
      "fun"            { yybegin(YYINITIAL); return FUN; }
      "acquires"       { return ACQUIRES; }
      "resource"       { yybegin(YYINITIAL); return RESOURCE; }
      "struct"         { yybegin(YYINITIAL); return STRUCT; }
      "use"            { yybegin(YYINITIAL); return USE; }
      "as"             { return AS; }
      "mut"            { return MUT; }
      "copyable"       { return COPYABLE; }
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

      "schema"         { return SCHEMA; }
      "define"         { return DEFINE; }

      "spec"           { yybegin(BEGIN_SPEC); return SPEC; }
}

<BEGIN_SPEC> {
    "fun"  { yybegin(IN_SPEC); return FUN; }
    "struct" { yybegin(IN_SPEC); return STRUCT; }
    "schema" { yybegin(IN_SPEC); return SCHEMA; }
    "define" { yybegin(IN_SPEC); return DEFINE; }
    "module" { yybegin(IN_SPEC); return MODULE; }

    [^...]     { yybegin(YYINITIAL); yypushback(yylength()); }
}

<IN_SPEC> {
  "global" / "("|"<"                        { return IDENTIFIER; }
  "global"                        { return GLOBAL; }

  "local"                          { return LOCAL; }
  "isolated"                        { return ISOLATED; }
  "deactivated"                        { return DEACTIVATED; }
  "concrete"                        { return CONCRETE; }
  "abstract"                        { return ABSTRACT; }

  "update" / "("|"<"                        { return IDENTIFIER; }
  "update"                        { return UPDATE; }

  "exists" / "("|"<"                        { return IDENTIFIER; }
  "exists"                        { return EXISTS; }

  "pragma"                         { return PRAGMA; }
  "assume"                         { return ASSUME; }
  "assert"                         { return ASSERT; }
  "aborts_if"                      { return ABORTS_IF; }
  "with"                           { return WITH; }
  "succeeds_if"                    { return SUCCEEDS_IF; }
  "requires"                       { return REQUIRES; }
  "ensures"                        { return ENSURES; }
  "modifies"                       { return MODIFIES; }
  "include"                        { return INCLUDE; }
  "internal"                       { return INTERNAL; }
  "invariant"                      { return INVARIANT; }
  "pack"                           { return PACK; }
  "unpack"                         { return UNPACK; }
  "apply"                          { return APPLY; }
  "to"                             { return TO; }
  "except"                         { return EXCEPT; }
  "forall"                         { return FORALL; }
  "in"                             { return IN; }
  "where"                             { return WHERE; }
}

<YYINITIAL, BEGIN_SPEC, IN_SPEC> {
  {ADDRESS_LITERAL}          { return ADDRESS_LITERAL; }
  {BECH32_ADDRESS_LITERAL}          { return BECH32_ADDRESS_LITERAL; }
  {BOOL_LITERAL}             { return BOOL_LITERAL; }
  {INTEGER_LITERAL}          { return INTEGER_LITERAL; }
  {HEX_STRING_LITERAL}       { return HEX_STRING_LITERAL; }
  {BYTE_STRING_LITERAL}      { return BYTE_STRING_LITERAL; }
  {IDENTIFIER}               { return IDENTIFIER; }
}

<IN_BLOCK_COMMENT> {
  "*/"    { popState(); return BLOCK_COMMENT; }
  <<EOF>> { popState(); return BLOCK_COMMENT; }
  [^]     { }
}

[^] { return BAD_CHARACTER; }
