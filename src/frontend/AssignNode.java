package frontend;

import exceptions.SysYException;
import utils.Pair;

import java.util.Optional;

public class AssignNode implements StmtNode {
    public final LValNode left;
    public final ExprNode right;

    public AssignNode(LValNode left, ExprNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return "(" + left.toString() + ") := (" + right.toString() + ")";
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        right.check(symbolTable, inLoop);
        left.check(symbolTable, inLoop);
        final Optional<VarDefNode> defNode = symbolTable.find(left.name);
        if (defNode.isPresent()) {
            if (!defNode.get().getInfo().get(left.name).first) {
                errors.add(Pair.of(left.line, SysYException.Code.h));
            }
        }
        return symbolTable;
    }

    @Override
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        return Pair.of(symbolTable, new AssignNode((LValNode) left.simplify(symbolTable).second,
                (ExprNode) right.simplify(symbolTable).second));
    }
}
