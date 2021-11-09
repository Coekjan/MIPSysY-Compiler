package frontend;

import utils.Pair;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public abstract class SyntaxTreeBuilder {
    public static GlobalNode fetch(ParserUnit parserUnit) {
        if (!parserUnit.name.equals("CompUnit")) {
            throw new IllegalArgumentException();
        }
        return toGlobal(parserUnit);
    }

    private static AssignNode toAssign(ParserUnit parserUnit) { // Stmt
        final LValNode left = toLVal(parserUnit.derivations.get(0));
        if (parserUnit.derivations.get(2).name.equals("Exp")) {
            return new AssignNode(left, toExpr(parserUnit.derivations.get(2)));
        }
        return new AssignNode(left, new FuncCallNode((Token) parserUnit.derivations.get(2), Collections.emptyList()));
    }
    
    private static BinaryExprNode toBinaryExpr(ParserUnit parserUnit) {
        switch (((Token) parserUnit.derivations.get(1)).type) {
            case PLUS:
                return new BinaryExprNode(BinaryExprNode.Operator.ADD,
                        toExpr(parserUnit.derivations.get(0)), toExpr(parserUnit.derivations.get(2)));
            case MINU:
                return new BinaryExprNode(BinaryExprNode.Operator.SUB,
                        toExpr(parserUnit.derivations.get(0)), toExpr(parserUnit.derivations.get(2)));
            case MULT:
                return new BinaryExprNode(BinaryExprNode.Operator.MUL,
                        toExpr(parserUnit.derivations.get(0)), toExpr(parserUnit.derivations.get(2)));
            case DIV:
                return new BinaryExprNode(BinaryExprNode.Operator.DIV,
                        toExpr(parserUnit.derivations.get(0)), toExpr(parserUnit.derivations.get(2)));
            case MOD:
                return new BinaryExprNode(BinaryExprNode.Operator.MOD,
                        toExpr(parserUnit.derivations.get(0)), toExpr(parserUnit.derivations.get(2)));
            case OR:
                return new BinaryExprNode(BinaryExprNode.Operator.OR,
                        toExpr(parserUnit.derivations.get(0)), toExpr(parserUnit.derivations.get(2)));
            case AND:
                return new BinaryExprNode(BinaryExprNode.Operator.AND,
                        toExpr(parserUnit.derivations.get(0)), toExpr(parserUnit.derivations.get(2)));
            case EQL:
                return new BinaryExprNode(BinaryExprNode.Operator.EQ,
                        toExpr(parserUnit.derivations.get(0)), toExpr(parserUnit.derivations.get(2)));
            case NEQ:
                return new BinaryExprNode(BinaryExprNode.Operator.NE,
                        toExpr(parserUnit.derivations.get(0)), toExpr(parserUnit.derivations.get(2)));
            case GRE:
                return new BinaryExprNode(BinaryExprNode.Operator.GT,
                        toExpr(parserUnit.derivations.get(0)), toExpr(parserUnit.derivations.get(2)));
            case GEQ:
                return new BinaryExprNode(BinaryExprNode.Operator.GE,
                        toExpr(parserUnit.derivations.get(0)), toExpr(parserUnit.derivations.get(2)));
            case LSS:
                return new BinaryExprNode(BinaryExprNode.Operator.LT,
                        toExpr(parserUnit.derivations.get(0)), toExpr(parserUnit.derivations.get(2)));
            case LEQ:
                return new BinaryExprNode(BinaryExprNode.Operator.LE,
                        toExpr(parserUnit.derivations.get(0)), toExpr(parserUnit.derivations.get(2)));
            default:
                throw new IllegalArgumentException();
        }
    }
    
    private static BlockNode toBlock(ParserUnit parserUnit) { // Stmt
        return new BlockNode(parserUnit.derivations.stream()
                .filter(u -> u.name.equals("BlockItem")).flatMap(u -> u.derivations.stream())
                .filter(u -> u.name.equals("Decl") || u.name.equals("Stmt"))
                .map(u -> u.name.equals("Decl") ? toDecl(u) : toStmt(u)).collect(Collectors.toList()));
    }
    
    private static BranchNode toBranch(ParserUnit parserUnit) {
        if (parserUnit.derivations.size() > 5) {
            return new BranchNode(toExpr(parserUnit.derivations.get(2)),
                    toStmt(parserUnit.derivations.get(4)), toStmt(parserUnit.derivations.get(6)));
        }
        return new BranchNode(toExpr(parserUnit.derivations.get(2)), toStmt(parserUnit.derivations.get(4)));
    }
    
    private static BreakNode toBreak(ParserUnit parserUnit) { // Stmt
        return new BreakNode(((Token) parserUnit.derivations.get(0)).line);
    }
    
    private static ConstNode toConst(ParserUnit parserUnit) { // Number
        return new ConstNode(Integer.parseInt(((Token) parserUnit.derivations.get(0)).content));
    }
    
    private static ContNode toCont(ParserUnit parserUnit) { // Stmt
        return new ContNode(((Token) parserUnit.derivations.get(0)).line);
    }
    
    private static DeclNode toDecl(ParserUnit parserUnit) { // Decl
        return new DeclNode(
                !parserUnit.derivations.get(0).name.equals("ConstDecl"),
                parserUnit.derivations.get(0).derivations.stream()
                        .filter(u -> u.name.equals("ConstDef") || u.name.equals("VarDef"))
                        .map(SyntaxTreeBuilder::toDef).collect(Collectors.toList()));
    }

    private static ExprNode toExpr(ParserUnit parserUnit) {
        switch (parserUnit.name) {
            case "Cond":
            case "Exp":
            case "ConstExp":
                return toExpr(parserUnit.derivations.get(0));
            case "AddExp":
            case "MulExp":
            case "LAndExp":
            case "LOrExp":
            case "EqExp":
            case "RelExp":
                if (parserUnit.derivations.size() == 1) return toExpr(parserUnit.derivations.get(0));
                return toBinaryExpr(parserUnit);
            case "UnaryExp":
                if (parserUnit.derivations.size() == 1) return toExpr(parserUnit.derivations.get(0));
                if (parserUnit.derivations.get(0).name.equals("UnaryOp")) return toUnaryExpr(parserUnit);
                return toFuncCall(parserUnit);
            case "PrimaryExp":
                if (parserUnit.derivations.size() == 1) return toExpr(parserUnit.derivations.get(0));
                return toExpr(parserUnit.derivations.get(1));
            case "LVal":
                return toLVal(parserUnit);
            case "Number":
                return toConst(parserUnit);
            default:
                throw new IllegalArgumentException();
        }
    }

    private static StmtNode toStmt(ParserUnit parserUnit) { // Stmt
        final int size = parserUnit.derivations.size();
        final ParserUnit first = parserUnit.derivations.get(0);
        if (size == 1 && first.name.equals("Block")) {
            return toBlock(first);
        }
        if (first instanceof Token) {
            switch (((Token) first).type) {
                case IFTK: return toBranch(parserUnit);
                case WHILETK: return toLoop(parserUnit);
                case BREAKTK: return toBreak(parserUnit);
                case CONTINUETK: return toCont(parserUnit);
                case RETURNTK: return toReturn(parserUnit);
                case PRINTFTK: return new PrintNode(((Token) first),
                        new StringExprNode((Token) parserUnit.derivations.get(2)),
                        parserUnit.derivations.stream()
                                .filter(u -> u.name.equals("Exp"))
                                .map(SyntaxTreeBuilder::toExpr).collect(Collectors.toList()));
                case SEMICN: return NopNode.NOP;
                default: throw new IllegalArgumentException();
            }
        }
        if (first.name.equals("LVal")) return toAssign(parserUnit);
        return toExpr(first);
    }
    
    private static FuncCallNode toFuncCall(ParserUnit parserUnit) {
        final Token identifier = (Token) parserUnit.derivations.get(0);
        if (parserUnit.derivations.size() > 3 || parserUnit.derivations.get(2).name.equals("FuncRParams")) {
            return new FuncCallNode(identifier, parserUnit.derivations.get(2).derivations.stream()
                    .filter(u -> u.name.equals("Exp"))
                    .map(SyntaxTreeBuilder::toExpr).collect(Collectors.toList()));
        }
        return new FuncCallNode(identifier, Collections.emptyList());
    }
    
    private static FuncDefNode toFuncDef(ParserUnit parserUnit) { // FuncDef
        boolean returnInt = ((Token) parserUnit.derivations.get(0).derivations.get(0)).type == Token.Type.INTTK;
        return new FuncDefNode(
                returnInt, (Token) parserUnit.derivations.get(1),
                !parserUnit.derivations.get(3).name.equals("FuncFParams") ? Collections.emptyList() :
                        parserUnit.derivations.get(3).derivations.stream().filter(u -> u.name.equals("FuncFParam"))
                                .map(SyntaxTreeBuilder::toFuncParam).collect(Collectors.toList()),
                toFuncBlock(parserUnit.derivations.get(parserUnit.derivations.size() - 1), returnInt));
    }

    private static FuncBlockNode toFuncBlock(ParserUnit parserUnit, boolean returnInt) {
        return new FuncBlockNode(parserUnit.derivations.stream()
                .filter(u -> u.name.equals("BlockItem")).flatMap(u -> u.derivations.stream())
                .filter(u -> u.name.equals("Decl") || u.name.equals("Stmt"))
                .map(u -> u.name.equals("Decl") ? toDecl(u) : toStmt(u)).collect(Collectors.toList()),
                returnInt, ((Token) parserUnit.derivations.get(parserUnit.derivations.size() - 1)).line);
    }

    private static FuncParamNode toFuncParam(ParserUnit parserUnit) { // FuncFParam
        final Token identifier = (Token) parserUnit.derivations.get(1);
        ExprNode dim1 = ConstNode.ZERO;
        ExprNode dim2 = ConstNode.ZERO;
        if (parserUnit.derivations.size() > 2) {
            dim1 = ConstNode.IGNR;
            if (parserUnit.derivations.size() > 4) {
                dim2 = toExpr(parserUnit.derivations.get(5));
            }
        }
        return new FuncParamNode(identifier, Pair.of(dim1, dim2));
    }
    
    private static GlobalNode toGlobal(ParserUnit parserUnit) { // CompUnit
        final List<DeclNode> declNodes = new LinkedList<>();
        final List<FuncDefNode> funcDefNodes = new LinkedList<>();
        for (ParserUnit unit : parserUnit.derivations) {
            if (unit.name.equals("MainFuncDef")) break;
            if (unit.name.equals("FuncDef")) funcDefNodes.add(toFuncDef(unit));
            else declNodes.add(toDecl(unit));
        }
        final ParserUnit mainBlock = parserUnit.derivations.get(parserUnit.derivations.size() - 1);
        return new GlobalNode(declNodes, funcDefNodes, new FuncDefNode(true,
                (Token) mainBlock.derivations.get(1),
                Collections.emptyList(),
                toFuncBlock(mainBlock.derivations.get(mainBlock.derivations.size() - 1), true)));
    }

    private static LoopNode toLoop(ParserUnit parserUnit) { // Stmt
        return new LoopNode(toExpr(parserUnit.derivations.get(2)), toStmt(parserUnit.derivations.get(4)));
    }

    private static LValNode toLVal(ParserUnit parserUnit) { // LVal
        final Token identifier = (Token) parserUnit.derivations.get(0);
        final List<ExprNode> exprNodes = parserUnit.derivations.stream()
                .filter(u -> u.name.equals("Exp"))
                .map(SyntaxTreeBuilder::toExpr).collect(Collectors.toList());
        if (exprNodes.isEmpty()) return new LValNode(identifier, Pair.of(ConstNode.ZERO, ConstNode.ZERO));
        if (exprNodes.size() == 1) return new LValNode(identifier, Pair.of(ConstNode.ZERO, exprNodes.get(0)));
        return new LValNode(identifier, Pair.of(exprNodes.get(0), exprNodes.get(1)));
    }

    private static ReturnNode toReturn(ParserUnit parserUnit) { // Stmt
        final int line = ((Token) parserUnit.derivations.get(0)).line;
        return parserUnit.derivations.size() <= 2 ? new ReturnNode(line) :
                new ReturnNode(line, toExpr(parserUnit.derivations.get(1)));
    }

    private static DefNode toDef(ParserUnit parserUnit) { // VarDef | ConstDef
        final Token identifier = (Token) parserUnit.derivations.get(0);
        ExprNode dim1 = ConstNode.ZERO;
        ExprNode dim2 = ConstNode.ZERO;
        final List<ExprNode> initValues = new LinkedList<>();
        for (int i = 1, end = parserUnit.derivations.size(); i < end; ++i) {
            final ParserUnit take = parserUnit.derivations.get(i);
            if (take instanceof Token && ((Token) take).type == Token.Type.LBRACK) {
                if (dim1 == ConstNode.ZERO) dim1 = toExpr(parserUnit.derivations.get(i + 1));
                else if (dim2 == ConstNode.ZERO) dim2 = toExpr(parserUnit.derivations.get(i + 1));
                i += 2;
            } else if (take instanceof Token && ((Token) take).type == Token.Type.ASSIGN) {
                final Queue<ParserUnit> queue = parserUnit.derivations.get(i + 1).derivations.stream()
                        .filter(u -> u.name.endsWith("InitVal") || u.name.endsWith("Exp"))
                        .collect(Collectors.toCollection(LinkedList::new));
                while (!queue.isEmpty()) {
                    final ParserUnit head = queue.poll();
                    if (head.name.endsWith("Exp")) {
                        initValues.add(toExpr(head));
                    } else queue.addAll(head.derivations.stream()
                            .filter(u -> u.name.endsWith("InitVal") || u.name.endsWith("Exp"))
                            .collect(Collectors.toList()));
                }
                break;
            }
        }
        return new DefNode(identifier, Pair.of(dim1, dim2), initValues);
    }

    private static UnaryExprNode toUnaryExpr(ParserUnit parserUnit) {
        switch (((Token) parserUnit.derivations.get(0).derivations.get(0)).type) {
            case PLUS:
                return new UnaryExprNode(UnaryExprNode.Operator.POS, toExpr(parserUnit.derivations.get(1)));
            case MINU:
                return new UnaryExprNode(UnaryExprNode.Operator.NEG, toExpr(parserUnit.derivations.get(1)));
            case NOT:
                return new UnaryExprNode(UnaryExprNode.Operator.NOT, toExpr(parserUnit.derivations.get(1)));
            default:
                throw new IllegalArgumentException();
        }
    }
}
