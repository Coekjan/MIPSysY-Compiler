package frontend;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Token extends ParserUnit {
    public enum Type {
        SPACE, SINGCOM, MULTCOM,
        IDENFR, INTCON, STRCON, MAINTK, CONSTTK, INTTK, BREAKTK, CONTINUETK, IFTK, ELSETK,
        NOT, AND, OR, WHILETK, GETINTTK, PRINTFTK, RETURNTK, PLUS, MINU, VOIDTK,
        MULT, DIV, MOD, LSS, LEQ, GRE, GEQ, EQL, NEQ,
        ASSIGN, SEMICN, COMMA, LPARENT, RPARENT, LBRACK, RBRACK, LBRACE, RBRACE;

        public static boolean ignore(Type type) {
            return type == SPACE || type == SINGCOM || type == MULTCOM;
        }
    }

    public static final Map<Type, String> typePatterns =
            Collections.unmodifiableMap(new LinkedHashMap<Type, String>() {{
                put(Type.MAINTK, "main(?![a-zA-Z0-9_])");
                put(Type.CONSTTK, "const(?![a-zA-Z0-9_])");
                put(Type.INTTK, "int(?![a-zA-Z0-9_])");
                put(Type.BREAKTK, "break(?![a-zA-Z0-9_])");
                put(Type.CONTINUETK, "continue(?![a-zA-Z0-9_])");
                put(Type.IFTK, "if(?![a-zA-Z0-9_])");
                put(Type.ELSETK, "else(?![a-zA-Z0-9_])");
                put(Type.WHILETK, "while(?![a-zA-Z0-9_])");
                put(Type.GETINTTK, "getint(?![a-zA-Z0-9_])");
                put(Type.PRINTFTK, "printf(?![a-zA-Z0-9_])");
                put(Type.RETURNTK, "return(?![a-zA-Z0-9_])");
                put(Type.VOIDTK, "void(?![a-zA-Z0-9_])");
                put(Type.SINGCOM, "//.*");
                put(Type.MULTCOM, "/\\*(.|\\n|\\r)*?\\*/");
                put(Type.AND, "&&");
                put(Type.OR, "\\|\\|");
                put(Type.PLUS, "\\+");
                put(Type.MINU, "-");
                put(Type.MULT, "\\*");
                put(Type.DIV, "/");
                put(Type.MOD, "%");
                put(Type.LEQ, "<=");
                put(Type.GEQ, ">=");
                put(Type.EQL, "==");
                put(Type.NEQ, "!=");
                put(Type.NOT, "!");
                put(Type.LSS, "<");
                put(Type.GRE, ">");
                put(Type.ASSIGN, "=");
                put(Type.SEMICN, ";");
                put(Type.COMMA, ",");
                put(Type.LPARENT, "\\(");
                put(Type.RPARENT, "\\)");
                put(Type.LBRACK, "\\[");
                put(Type.RBRACK, "\\]");
                put(Type.LBRACE, "\\{");
                put(Type.RBRACE, "\\}");
                put(Type.IDENFR, "[a-zA-Z_][a-zA-Z_0-9]*");
                put(Type.INTCON, "[1-9][0-9]*|0");
                put(Type.STRCON, "\".*?\"");
                put(Type.SPACE, "\\s+");
            }});

    public final String content;
    public final Type type;
    public final int line;

    public Token(String content, Type type, int line) {
        super(type.name(), Collections.emptyList());
        this.content = content;
        this.type = type;
        this.line = line;
    }

    @Override
    public String toString() {
        return type.toString() + " " + content;
    }
}
