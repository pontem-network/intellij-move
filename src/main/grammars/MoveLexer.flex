package org.move.lang;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import java.util.ArrayList;
import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.move.lang.MoveElementTypes.*;

%%

%{
    public int commentDepth = 0;
//  public ArrayList<Integer> stateStack = new ArrayList<Integer>();
%}

%{
public _MoveLexer() {
    this((java.io.Reader)null);
}

//public void maybeEndBlockComment() {
//    if (commentDepth == 0) {
//        yybegin(YYINITIAL);
//        return BLOCK_COMMENT;
//    }
//}
//public void pushState(int state) {
//    System.out.println("pushState: " + yystate());
//    this.stateStack.add(yystate());
//    yybegin(state);
//    System.out.println(stateStack);
//}

//public void popState() {
//    System.out.println("popState: " + yystate());
//    int oldState = this.stateStack.remove(this.stateStack.size() - 1);
//    yybegin(oldState);
//    System.out.println(stateStack);
//}

//public int stackSize() {
//    return this.stateStack.size();
//}
%}

%public
%class _MoveLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

%s IN_BLOCK_COMMENT
//%s BEGIN_SPEC
//%s IN_SPEC
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
//LINE_COMMENT=("//".*)|("//".*)

///////////////////////////////////////////////////////////////////////////////////////////////////
// Literals
///////////////////////////////////////////////////////////////////////////////////////////////////
PLACEHOLDER_ADDRESS_IDENT=\{\{[_a-zA-Z][_a-zA-Z0-9]*\}\}
PLACEHOLDER_ADDRESS_LITERAL=@\{\{[_a-zA-Z][_a-zA-Z0-9]*\}\}

ADDRESS_IDENT=0x[0-9a-fA-F]{1,40}
ADDRESS_LITERAL=@0x[0-9a-fA-F]{1,40}
BECH32_ADDRESS_IDENT=wallet1[A-Z0-9a-z&&[^boi1]]{6,83}
BECH32_ADDRESS_LITERAL=@wallet1[A-Z0-9a-z&&[^boi1]]{6,83}
POLKADOT_ADDRESS_IDENT=[1-9A-HJ-NP-Za-km-z]{40}[1-9A-HJ-NP-Za-km-z]*
POLKADOT_ADDRESS_LITERAL=@[1-9A-HJ-NP-Za-km-z]{40}[1-9A-HJ-NP-Za-km-z]*

BOOL_LITERAL=(true)|(false)
HEX_INTEGER_LITERAL=0x[0-9a-fA-F]+((u8)|(u64)|(u128))?
INTEGER_LITERAL=[0-9]+((u8)|(u64)|(u128))?
HEX_STRING_LITERAL=x\"([A-F0-9a-f]*)\"
BYTE_STRING_LITERAL=b\"(.*)\"

IDENTIFIER=[_a-zA-Z][_a-zA-Z0-9]*
//FUNCTION_PATTERN_NAME=[*_a-zA-Z][*_a-zA-Z0-9]*

%%
<YYINITIAL> {
      {WHITE_SPACE}        { return WHITE_SPACE; }
      "//" .*              { return LINE_COMMENT; }
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
//      "address"        { return ADDRESS; }
      "script"         { yybegin(YYINITIAL); return SCRIPT; }
      "module"         { yybegin(YYINITIAL); return MODULE; }
      "const"          { yybegin(YYINITIAL); return CONST; }
      "native"         { yybegin(YYINITIAL); return NATIVE; }
      "public"         { yybegin(YYINITIAL); return PUBLIC; }
      "fun"            { yybegin(YYINITIAL); return FUN; }
      "acquires"       { return ACQUIRES; }
      "struct"         { yybegin(YYINITIAL); return STRUCT; }
      "use"            { yybegin(YYINITIAL); return USE; }
      "as"             { return AS; }
      "has"             { return HAS; }
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
//      "friend"            { return FRIEND; }
      "phantom"            { return PHANTOM; }

      "schema"         { return SCHEMA; }
//      "define"         { return DEFINE; }

      "spec"           { return SPEC; }
}

//<BEGIN_SPEC> {
//    "fun"  { yybegin(IN_SPEC); return FUN; }
//    "struct" { yybegin(IN_SPEC); return STRUCT; }
//    "schema" { yybegin(IN_SPEC); return SCHEMA; }
//    "define" { yybegin(IN_SPEC); return DEFINE; }
//    "module" { yybegin(IN_SPEC); return MODULE; }
//    "spec"           { return SPEC; }
//
//    [^...]     { yybegin(YYINITIAL); yypushback(yylength()); }
//}

//<IN_SPEC> {
//  "global" / "("|"<"                        { return IDENTIFIER; }
//  "global"                        { return GLOBAL; }
//
//  "local"                          { return LOCAL; }
//  "isolated"                        { return ISOLATED; }
//  "deactivated"                        { return DEACTIVATED; }
//  "concrete"                        { return CONCRETE; }
//  "abstract"                        { return ABSTRACT; }
//
//  "update" / "("|"<"                        { return IDENTIFIER; }
//  "update"                        { return UPDATE; }
//
//  "exists" / "("|"<"                        { return IDENTIFIER; }
//  "exists"                        { return EXISTS; }
//
//  "pragma"                         { return PRAGMA; }
//  "assume"                         { return ASSUME; }
//  "assert"                         { return ASSERT; }
//  "aborts_if"                      { return ABORTS_IF; }
//  "with"                           { return WITH; }
//  "succeeds_if"                    { return SUCCEEDS_IF; }
//  "requires"                       { return REQUIRES; }
//  "ensures"                        { return ENSURES; }
//  "modifies"                       { return MODIFIES; }
//  "include"                        { return INCLUDE; }
//  "internal"                       { return INTERNAL; }
//  "invariant"                      { return INVARIANT; }
//  "pack"                           { return PACK; }
//  "unpack"                         { return UNPACK; }
//  "apply"                          { return APPLY; }
//  "emits"                          { return EMITS; }
//  "to"                             { return TO; }
//  "except"                         { return EXCEPT; }
//  "forall"                         { return FORALL; }
//  "in"                             { return IN; }
//  "where"                             { return WHERE; }
//}

<YYINITIAL> {
  {PLACEHOLDER_ADDRESS_IDENT}          { return PLACEHOLDER_ADDRESS_IDENT; }
  {PLACEHOLDER_ADDRESS_LITERAL}          { return PLACEHOLDER_ADDRESS_LITERAL; }
  {ADDRESS_LITERAL}          { return ADDRESS_LITERAL; }
  {BECH32_ADDRESS_LITERAL}          { return BECH32_ADDRESS_LITERAL; }
  {POLKADOT_ADDRESS_LITERAL}          { return POLKADOT_ADDRESS_LITERAL; }
  {ADDRESS_IDENT}          { return ADDRESS_IDENT; }
  {BECH32_ADDRESS_IDENT}          { return BECH32_ADDRESS_IDENT; }
  {POLKADOT_ADDRESS_IDENT}          { return POLKADOT_ADDRESS_IDENT; }

  {BOOL_LITERAL}             { return BOOL_LITERAL; }
  {HEX_INTEGER_LITERAL}          { return HEX_INTEGER_LITERAL; }
  {INTEGER_LITERAL}          { return INTEGER_LITERAL; }
  {HEX_STRING_LITERAL}       { return HEX_STRING_LITERAL; }
  {BYTE_STRING_LITERAL}      { return BYTE_STRING_LITERAL; }
  {IDENTIFIER}               { return IDENTIFIER; }
}

<IN_BLOCK_COMMENT> {
  "/*"    {
          commentDepth += 1;
//          pushState(IN_BLOCK_COMMENT);
      }
  "*/"    {
          commentDepth -= 1;
    if (commentDepth == 0) {
        yybegin(YYINITIAL);
        return BLOCK_COMMENT;
    }
//          popState();
//          if (stackSize() == 0) return BLOCK_COMMENT;
      }
  <<EOF>> {     if (commentDepth == 0) {
                    yybegin(YYINITIAL);
                    return BLOCK_COMMENT;
                }
 }
  [^]     { }
}

[^] { return BAD_CHARACTER; }
