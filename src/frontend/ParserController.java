package frontend;

import exceptions.SysYException;
import utils.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static frontend.Token.Type.*;

public class ParserController {
    public static final List<Pair<Integer, SysYException.Code>> errors = new LinkedList<>();

    public static class ParseError extends Exception {
        public final Token.Type type;

        public ParseError() {
            this(null);
        }
        
        public ParseError(Token.Type type) {
            this.type = type;
        }
    }

    private static Token takeWithAssert(TokenSupporter supporter, Token.Type type) throws ParseError {
        final Token take = supporter.take().orElseThrow(() -> new ParseError(type));
        if (take.type != type) {
            supporter.pushBack();
            throw new ParseError(type);
        }
        return take;
    }

    public static class CompUnit {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            while (supporter.size() >= 3 && (
                    supporter.get(0).type == CONSTTK ||
                    supporter.get(0).type == INTTK && supporter.get(1).type != MAINTK && supporter.get(2).type != LPARENT)
            ) {
                units.add(Decl.parse(supporter));
            }
            while (supporter.size() >= 3 && (
                    supporter.get(0).type == VOIDTK ||
                    supporter.get(0).type == INTTK && supporter.get(1).type != MAINTK && supporter.get(2).type == LPARENT)
            ) {
                units.add(FuncDef.parse(supporter));
            }
            units.add(MainFuncDef.parse(supporter));
            return new ParserUnit("CompUnit", units);
        }
    }

    public static class Decl {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            if (supporter.get(0).type == CONSTTK) {
                units.add(ConstDecl.parse(supporter));
            } else {
                units.add(VarDecl.parse(supporter));
            }
            return new ParserUnit("Decl", units);
        }
    }

    public static class ConstDecl {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(takeWithAssert(supporter, CONSTTK));
            units.add(BType.parse(supporter));
            units.add(ConstDef.parse(supporter));
            while (supporter.size() > 0 && supporter.get(0).type == COMMA) {
                units.add(takeWithAssert(supporter, COMMA));
                units.add(ConstDef.parse(supporter));
            }
            try {
                units.add(takeWithAssert(supporter, SEMICN));
            } catch (ParseError e) {
                final Optional<Token> prev = supporter.prev();
                assert prev.isPresent();
                ParserController.errors.add(Pair.of(prev.get().line, SysYException.Code.i));
            }
            return new ParserUnit("ConstDecl", units);
        }
    }

    public static class BType {
        public static ParserUnit parse(TokenSupporter supporter) throws ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(takeWithAssert(supporter, INTTK));
            return new ParserUnit("BType", units);
        }
    }

    public static class ConstDef {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(takeWithAssert(supporter, IDENFR));
            while (supporter.size() > 0 && supporter.get(0).type == LBRACK) {
                units.add(takeWithAssert(supporter, LBRACK));
                units.add(ConstExp.parse(supporter));
                try {
                    units.add(takeWithAssert(supporter, RBRACK));
                } catch (ParseError e) {
                    final Optional<Token> prev = supporter.prev();
                    assert prev.isPresent();
                    ParserController.errors.add(Pair.of(prev.get().line, SysYException.Code.k));
                }
            }
            units.add(takeWithAssert(supporter, ASSIGN));
            units.add(ConstInitVal.parse(supporter));
            return new ParserUnit("ConstDef", units);
        }
    }

    public static class ConstInitVal {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            if (supporter.size() > 0 && supporter.get(0).type == LBRACE) {
                units.add(takeWithAssert(supporter, LBRACE));
                if (supporter.isEmpty() || supporter.get(0).type != RBRACE) {
                    units.add(ConstInitVal.parse(supporter));
                    while (supporter.size() > 0 && supporter.get(0).type == COMMA) {
                        units.add(takeWithAssert(supporter, COMMA));
                        units.add(ConstInitVal.parse(supporter));
                    }
                }
                units.add(takeWithAssert(supporter, RBRACE));
            } else {
                units.add(ConstExp.parse(supporter));
            }
            return new ParserUnit("ConstInitVal", units);
        }
    }

    public static class VarDecl {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(BType.parse(supporter));
            units.add(VarDef.parse(supporter));
            while (supporter.size() > 0 && supporter.get(0).type == COMMA) {
                units.add(takeWithAssert(supporter, COMMA));
                units.add(VarDef.parse(supporter));
            }
            try {
                units.add(takeWithAssert(supporter, SEMICN));
            } catch (ParseError e) {
                final Optional<Token> prev = supporter.prev();
                assert prev.isPresent();
                ParserController.errors.add(Pair.of(prev.get().line, SysYException.Code.i));
            }
            return new ParserUnit("VarDecl", units);
        }
    }

    public static class VarDef {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(takeWithAssert(supporter, IDENFR));
            while (supporter.size() > 0 && supporter.get(0).type == LBRACK) {
                units.add(takeWithAssert(supporter, LBRACK));
                units.add(ConstExp.parse(supporter));
                try {
                    units.add(takeWithAssert(supporter, RBRACK));
                } catch (ParseError e) {
                    final Optional<Token> prev = supporter.prev();
                    assert prev.isPresent();
                    ParserController.errors.add(Pair.of(prev.get().line, SysYException.Code.k));
                }
            }
            if (supporter.size() > 0 && supporter.get(0).type == ASSIGN) {
                units.add(takeWithAssert(supporter, ASSIGN));
                units.add(InitVal.parse(supporter));
            }
            return new ParserUnit("VarDef", units);
        }
    }

    public static class InitVal {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            if (supporter.size() > 0 && supporter.get(0).type == LBRACE) {
                units.add(takeWithAssert(supporter, LBRACE));
                if (supporter.isEmpty() || supporter.get(0).type != RBRACE) {
                    units.add(InitVal.parse(supporter));
                    while (supporter.size() > 0 && supporter.get(0).type == COMMA) {
                        units.add(takeWithAssert(supporter, COMMA));
                        units.add(InitVal.parse(supporter));
                    }
                }
                units.add(takeWithAssert(supporter, RBRACE));
            } else {
                units.add(Exp.parse(supporter));
            }
            return new ParserUnit("InitVal", units);
        }
    }

    public static class FuncDef {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(FuncType.parse(supporter));
            units.add(takeWithAssert(supporter, IDENFR));
            units.add(takeWithAssert(supporter, LPARENT));
            if (supporter.size() > 0 && supporter.get(0).type == INTTK) {
                units.add(FuncFParams.parse(supporter));
            }
            try {
                units.add(takeWithAssert(supporter, RPARENT));
            } catch (ParseError e) {
                final Optional<Token> prev = supporter.prev();
                assert prev.isPresent();
                ParserController.errors.add(Pair.of(prev.get().line, SysYException.Code.j));
            }
            units.add(Block.parse(supporter));
            return new ParserUnit("FuncDef", units);
        }
    }

    public static class MainFuncDef {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(takeWithAssert(supporter, INTTK));
            units.add(takeWithAssert(supporter, MAINTK));
            units.add(takeWithAssert(supporter, LPARENT));
            try {
                units.add(takeWithAssert(supporter, RPARENT));
            } catch (ParseError e) {
                final Optional<Token> prev = supporter.prev();
                assert prev.isPresent();
                ParserController.errors.add(Pair.of(prev.get().line, SysYException.Code.j));
            }
            units.add(Block.parse(supporter));
            return new ParserUnit("MainFuncDef", units);
        }
    }

    public static class FuncType {
        public static ParserUnit parse(TokenSupporter supporter) throws ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            if (supporter.size() > 0 && supporter.get(0).type == VOIDTK) {
                units.add(takeWithAssert(supporter, VOIDTK));
            } else {
                units.add(takeWithAssert(supporter, INTTK));
            }
            return new ParserUnit("FuncType", units);
        }
    }

    public static class FuncFParams {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(FuncFParam.parse(supporter));
            while (supporter.size() > 0 && supporter.get(0).type == COMMA) {
                units.add(takeWithAssert(supporter, COMMA));
                units.add(FuncFParam.parse(supporter));
            }
            return new ParserUnit("FuncFParams", units);
        }
    }

    public static class FuncFParam {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(BType.parse(supporter));
            units.add(takeWithAssert(supporter, IDENFR));
            if (supporter.size() > 0 && supporter.get(0).type == LBRACK) {
                units.add(takeWithAssert(supporter, LBRACK));
                try {
                    units.add(takeWithAssert(supporter, RBRACK));
                } catch (ParseError e) {
                    final Optional<Token> prev = supporter.prev();
                    assert prev.isPresent();
                    ParserController.errors.add(Pair.of(prev.get().line, SysYException.Code.k));
                }
                while (supporter.size() > 0 && supporter.get(0).type == LBRACK) {
                    units.add(takeWithAssert(supporter, LBRACK));
                    units.add(ConstExp.parse(supporter));
                    try {
                        units.add(takeWithAssert(supporter, RBRACK));
                    } catch (ParseError e) {
                        final Optional<Token> prev = supporter.prev();
                        assert prev.isPresent();
                        ParserController.errors.add(Pair.of(prev.get().line, SysYException.Code.k));
                    }
                }
            }
            return new ParserUnit("FuncFParam", units);
        }
    }

    public static class Block {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(takeWithAssert(supporter, LBRACE));
            while (supporter.size() > 0 && supporter.get(0).type != RBRACE) {
                units.add(BlockItem.parse(supporter));
            }
            units.add(takeWithAssert(supporter, RBRACE));
            return new ParserUnit("Block", units);
        }
    }

    public static class BlockItem {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            if (supporter.size() > 0 && (supporter.get(0).type == CONSTTK || supporter.get(0).type == INTTK)) {
                units.add(Decl.parse(supporter));
            } else {
                units.add(Stmt.parse(supporter));
            }
            return new ParserUnit("BlockItem", units);
        }
    }

    public static class Stmt {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            if (supporter.isEmpty()) {
                throw new ParseError();
            }
            switch (supporter.get(0).type) {
                case LBRACE:
                    units.add(Block.parse(supporter));
                    break;
                case IFTK:
                    units.add(takeWithAssert(supporter, IFTK));
                    units.add(takeWithAssert(supporter, LPARENT));
                    units.add(Cond.parse(supporter));
                    try {
                        units.add(takeWithAssert(supporter, RPARENT));
                    } catch (ParseError e) {
                        final Optional<Token> prev = supporter.prev();
                        assert prev.isPresent();
                        ParserController.errors.add(Pair.of(prev.get().line, SysYException.Code.j));
                    }
                    units.add(Stmt.parse(supporter));
                    if (supporter.size() > 0 && supporter.get(0).type == ELSETK) {
                        units.add(takeWithAssert(supporter, ELSETK));
                        units.add(Stmt.parse(supporter));
                    }
                    break;
                case WHILETK:
                    units.add(takeWithAssert(supporter, WHILETK));
                    units.add(takeWithAssert(supporter, LPARENT));
                    units.add(Cond.parse(supporter));
                    try {
                        units.add(takeWithAssert(supporter, RPARENT));
                    } catch (ParseError e) {
                        final Optional<Token> prev = supporter.prev();
                        assert prev.isPresent();
                        ParserController.errors.add(Pair.of(prev.get().line, SysYException.Code.j));
                    }
                    units.add(Stmt.parse(supporter));
                    break;
                case BREAKTK:
                    units.add(takeWithAssert(supporter, BREAKTK));
                    try {
                        units.add(takeWithAssert(supporter, SEMICN));
                    } catch (ParseError e) {
                        final Optional<Token> prev = supporter.prev();
                        assert prev.isPresent();
                        ParserController.errors.add(Pair.of(prev.get().line, SysYException.Code.i));
                    }
                    break;
                case CONTINUETK:
                    units.add(takeWithAssert(supporter, CONTINUETK));
                    try {
                        units.add(takeWithAssert(supporter, SEMICN));
                    } catch (ParseError e) {
                        final Optional<Token> prev = supporter.prev();
                        assert prev.isPresent();
                        ParserController.errors.add(Pair.of(prev.get().line, SysYException.Code.i));
                    }
                    break;
                case RETURNTK:
                    units.add(takeWithAssert(supporter, RETURNTK));
                    try {
                        final TokenSupporter tempSupporter = supporter.clone();
                        Exp.parse(tempSupporter);
                        units.add(Exp.parse(supporter));
                    } catch (ParseError ignored) {
                    }
                    try {
                        units.add(takeWithAssert(supporter, SEMICN));
                    } catch (ParseError e) {
                        final Optional<Token> prev = supporter.prev();
                        assert prev.isPresent();
                        ParserController.errors.add(Pair.of(prev.get().line, SysYException.Code.i));
                    }
                    break;
                case PRINTFTK:
                    units.add(takeWithAssert(supporter, PRINTFTK));
                    units.add(takeWithAssert(supporter, LPARENT));
                    units.add(takeWithAssert(supporter, STRCON));
                    while (supporter.size() > 0 && supporter.get(0).type == COMMA) {
                        units.add(takeWithAssert(supporter, COMMA));
                        units.add(Exp.parse(supporter));
                    }
                    try {
                        units.add(takeWithAssert(supporter, RPARENT));
                    } catch (ParseError e) {
                        final Optional<Token> prev = supporter.prev();
                        assert prev.isPresent();
                        ParserController.errors.add(Pair.of(prev.get().line, SysYException.Code.j));
                    }
                    try {
                        units.add(takeWithAssert(supporter, SEMICN));
                    } catch (ParseError e) {
                        final Optional<Token> prev = supporter.prev();
                        assert prev.isPresent();
                        ParserController.errors.add(Pair.of(prev.get().line, SysYException.Code.i));
                    }
                    break;
                default:
                    try {
                        final TokenSupporter tempSupporter = supporter.clone();
                        LVal.parse(tempSupporter);
                        takeWithAssert(tempSupporter, ASSIGN);
                        units.add(LVal.parse(supporter));
                        units.add(takeWithAssert(supporter, ASSIGN));
                        if (supporter.size() > 0 && supporter.get(0).type == GETINTTK) {
                            units.add(takeWithAssert(supporter, GETINTTK));
                            units.add(takeWithAssert(supporter, LPARENT));
                            try {
                                units.add(takeWithAssert(supporter, RPARENT));
                            } catch (ParseError e) {
                                final Optional<Token> prev = supporter.prev();
                                assert prev.isPresent();
                                ParserController.errors.add(Pair.of(prev.get().line, SysYException.Code.j));
                            }
                        } else {
                            units.add(Exp.parse(supporter));
                        }
                    } catch (ParseError e) {
                        if (supporter.size() > 0 && supporter.get(0).type != SEMICN) {
                            units.add(Exp.parse(supporter));
                        }
                    }
                    try {
                        units.add(takeWithAssert(supporter, SEMICN));
                    } catch (ParseError e) {
                        final Optional<Token> prev = supporter.prev();
                        assert prev.isPresent();
                        ParserController.errors.add(Pair.of(prev.get().line, SysYException.Code.i));
                    }
            }
            return new ParserUnit("Stmt", units);
        }
    }

    public static class Exp {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(AddExp.parse(supporter));
            return new ParserUnit("Exp", units);
        }
    }

    public static class Cond {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(LOrExp.parse(supporter));
            return new ParserUnit("Cond", units);
        }
    }

    public static class LVal {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(takeWithAssert(supporter, IDENFR));
            while (supporter.size() > 0 && supporter.get(0).type == LBRACK) {
                units.add(takeWithAssert(supporter, LBRACK));
                units.add(Exp.parse(supporter));
                try {
                    units.add(takeWithAssert(supporter, RBRACK));
                } catch (ParseError e) {
                    final Optional<Token> prev = supporter.prev();
                    assert prev.isPresent();
                    ParserController.errors.add(Pair.of(prev.get().line, SysYException.Code.k));
                }
            }
            return new ParserUnit("LVal", units);
        }
    }

    public static class PrimaryExp {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            if (supporter.isEmpty()) throw new ParseError();
            if (supporter.get(0).type == LPARENT) {
                units.add(takeWithAssert(supporter, LPARENT));
                units.add(Exp.parse(supporter));
                try {
                    units.add(takeWithAssert(supporter, RPARENT));
                } catch (ParseError e) {
                    final Optional<Token> prev = supporter.prev();
                    assert prev.isPresent();
                    ParserController.errors.add(Pair.of(prev.get().line, SysYException.Code.j));
                }
            } else if (supporter.get(0).type == IDENFR) {
                units.add(LVal.parse(supporter));
            } else {
                units.add(Number.parse(supporter));
            }
            return new ParserUnit("PrimaryExp", units);
        }
    }

    public static class Number {
        public static ParserUnit parse(TokenSupporter supporter) throws ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(takeWithAssert(supporter, INTCON));
            return new ParserUnit("Number", units);
        }
    }

    public static class UnaryExp {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            if (supporter.isEmpty()) throw new ParseError();
            if (supporter.get(0).type == PLUS || supporter.get(0).type == MINU || supporter.get(0).type == NOT) {
                units.add(UnaryOp.parse(supporter));
                units.add(UnaryExp.parse(supporter));
            } else if (supporter.size() > 1 && supporter.get(0).type == IDENFR && supporter.get(1).type == LPARENT) {
                units.add(takeWithAssert(supporter, IDENFR));
                units.add(takeWithAssert(supporter, LPARENT));
                try {
                    final TokenSupporter tempSupporter = supporter.clone();
                    Exp.parse(tempSupporter);
                    units.add(FuncRParams.parse(supporter));
                } catch (ParseError ignored) {
                }
                try {
                    units.add(takeWithAssert(supporter, RPARENT));
                } catch (ParseError e) {
                    final Optional<Token> prev = supporter.prev();
                    assert prev.isPresent();
                    ParserController.errors.add(Pair.of(prev.get().line, SysYException.Code.j));
                }
            } else {
                units.add(PrimaryExp.parse(supporter));
            }
            return new ParserUnit("UnaryExp", units);
        }
    }

    public static class UnaryOp {
        public static ParserUnit parse(TokenSupporter supporter) throws ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            if (supporter.isEmpty()) throw new ParseError();
            switch (supporter.get(0).type) {
                case PLUS:
                    units.add(takeWithAssert(supporter, PLUS));
                    break;
                case MINU:
                    units.add(takeWithAssert(supporter, MINU));
                    break;
                case NOT:
                    units.add(takeWithAssert(supporter, NOT));
                    break;
                default:
                    throw new ParseError();
            }
            return new ParserUnit("UnaryOp", units);
        }
    }

    public static class FuncRParams {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(Exp.parse(supporter));
            while (supporter.size() > 0 && supporter.get(0).type == COMMA) {
                units.add(takeWithAssert(supporter, COMMA));
                units.add(Exp.parse(supporter));
            }
            return new ParserUnit("FuncRParams", units);
        }
    }

    public static class MulExp {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(UnaryExp.parse(supporter));
            if (supporter.isEmpty()) throw new ParseError();
            while (supporter.get(0).type == MULT || supporter.get(0).type == DIV || supporter.get(0).type == MOD) {
                final ParserUnit comb = new ParserUnit("MulExp", units);
                units.clear();
                units.add(comb);
                units.add(takeWithAssert(supporter, supporter.get(0).type));
                units.add(UnaryExp.parse(supporter));
            }
            return new ParserUnit("MulExp", units);
        }
    }

    public static class AddExp {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(MulExp.parse(supporter));
            if (supporter.isEmpty()) throw new ParseError();
            while (supporter.get(0).type == PLUS || supporter.get(0).type == MINU) {
                final ParserUnit comb = new ParserUnit("AddExp", units);
                units.clear();
                units.add(comb);
                units.add(takeWithAssert(supporter, supporter.get(0).type));
                units.add(MulExp.parse(supporter));
            }
            return new ParserUnit("AddExp", units);
        }
    }

    public static class RelExp {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(AddExp.parse(supporter));
            if (supporter.isEmpty()) throw new ParseError();
            while (supporter.get(0).type == LSS || supporter.get(0).type == GRE ||
                    supporter.get(0).type == LEQ || supporter.get(0).type == GEQ) {
                final ParserUnit comb = new ParserUnit("RelExp", units);
                units.clear();
                units.add(comb);
                units.add(takeWithAssert(supporter, supporter.get(0).type));
                units.add(AddExp.parse(supporter));
            }
            return new ParserUnit("RelExp", units);
        }
    }

    public static class EqExp {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(RelExp.parse(supporter));
            if (supporter.isEmpty()) throw new ParseError();
            while (supporter.get(0).type == EQL || supporter.get(0).type == NEQ) {
                final ParserUnit comb = new ParserUnit("EqExp", units);
                units.clear();
                units.add(comb);
                units.add(takeWithAssert(supporter, supporter.get(0).type));
                units.add(RelExp.parse(supporter));
            }
            return new ParserUnit("EqExp", units);
        }
    }

    public static class LAndExp {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(EqExp.parse(supporter));
            if (supporter.isEmpty()) throw new ParseError();
            while (supporter.get(0).type == AND) {
                final ParserUnit comb = new ParserUnit("LAndExp", units);
                units.clear();
                units.add(comb);
                units.add(takeWithAssert(supporter, AND));
                units.add(EqExp.parse(supporter));
            }
            return new ParserUnit("LAndExp", units);
        }
    }

    public static class LOrExp {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(LAndExp.parse(supporter));
            if (supporter.isEmpty()) throw new ParseError();
            while (supporter.get(0).type == OR) {
                final ParserUnit comb = new ParserUnit("LOrExp", units);
                units.clear();
                units.add(comb);
                units.add(takeWithAssert(supporter, OR));
                units.add(LAndExp.parse(supporter));
            }
            return new ParserUnit("LOrExp", units);
        }
    }

    public static class ConstExp {
        public static ParserUnit parse(TokenSupporter supporter) throws SysYException, ParseError {
            final List<ParserUnit> units = new LinkedList<>();
            units.add(AddExp.parse(supporter));
            return new ParserUnit("ConstExp", units);
        }
    }
}
