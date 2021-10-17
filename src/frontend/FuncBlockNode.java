package frontend;

import exceptions.SysYException;
import utils.Pair;

import java.util.List;

public class FuncBlockNode extends BlockNode {
    public final boolean hasReturn;
    public final int endLine;

    public FuncBlockNode(List<BlockItemNode> items, boolean hasReturn, int endLine) {
        super(items);
        this.hasReturn = hasReturn;
        this.endLine = endLine;
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        super.check(symbolTable, inLoop);
        if (items.isEmpty() || !(items.get(items.size() - 1) instanceof ReturnNode)) {
            errors.add(Pair.of(endLine, SysYException.Code.g));
        }
        final List<ReturnNode> returnNodes = findRet();
        if (!hasReturn) {
            for (ReturnNode node : returnNodes) {
                if (node.returnValue != null) {
                    errors.add(Pair.of(node.line, SysYException.Code.f));
                }
            }
        }
        return symbolTable;
    }
}
