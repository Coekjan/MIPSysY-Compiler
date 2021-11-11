package frontend;

import exceptions.SysYException;
import midend.IntermediateCode;
import midend.LabelTable;
import midend.Nop;
import utils.Pair;

public class NopNode implements StmtNode {
    public static final NopNode NOP = new NopNode();

    private NopNode() {
    }

    @Override
    public String toString() {
        return "NOP";
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        return symbolTable;
    }

    @Override
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        return Pair.of(symbolTable, this);
    }

    @Override
    public Pair<SymbolTable, ICodeInfo> iCode(LabelTable lt, SymbolTable st, String lpBegin, String lpEnd, int tc) {
        final IntermediateCode nop = new Nop();
        return Pair.of(st, new ICodeInfo(nop, nop, null, tc));
    }
}
