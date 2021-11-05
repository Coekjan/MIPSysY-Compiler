package frontend;

import exceptions.SysYException;
import utils.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UnaryExprNode implements ExprNode {
    public enum Operator {
        POS, NEG, NOT
    }

    @FunctionalInterface
    private interface ExecUnaOperator {
        int exec(int x);
    }

    public final Operator operator;
    public final ExprNode exprNode;

    public UnaryExprNode(Operator operator, ExprNode exprNode) {
        this.operator = operator;
        this.exprNode = exprNode;
    }

    private int operate(int x) {
        final Map<Operator, ExecUnaOperator> operations =
                Collections.unmodifiableMap(new HashMap<Operator, ExecUnaOperator>() {{
                    put(Operator.POS, a -> a);
                    put(Operator.NEG, a -> -a);
                    put(Operator.NOT, a -> a == 0 ? 1 : 0);
                }});
        return operations.get(operator).exec(x);
    }

    @Override
    public String toString() {
        return operator.toString() + " (" + exprNode.toString() + ")";
    }

    @Override
    public ReturnType getRetType(SymbolTable symbolTable) throws SysYException {
        if (exprNode.getRetType(symbolTable) == ReturnType.INT) return ReturnType.INT;
        throw new SysYException(SysYException.Code.e);
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        exprNode.check(symbolTable, inLoop);
        return symbolTable;
    }

    @Override
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        final ExprNode simExpr = (ExprNode) exprNode.simplify(symbolTable).second;
        if (simExpr instanceof ConstNode) {
            return Pair.of(symbolTable, new ConstNode(operate(((ConstNode) simExpr).constant)));
        }
        return Pair.of(symbolTable, new UnaryExprNode(operator, simExpr));
    }
}
