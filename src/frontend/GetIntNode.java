package frontend;

import exceptions.SysYException;
import midend.*;
import utils.Pair;

import java.util.Collections;

public class GetIntNode extends FuncCallNode {
    public GetIntNode(Token identifier) {
        super(identifier, Collections.emptyList());
    }

    public GetIntNode(String name, int line) {
        super(name, line, Collections.emptyList());
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
    public ReturnType getRetType(SymbolTable symbolTable) {
        return ReturnType.INT;
    }

    @Override
    public Pair<SymbolTable, ICodeInfo> iCode(LabelTable lt, SymbolTable st, String lpBegin, String lpEnd, int tc) {
        final IntermediateCode head = new GetInt();
        final Value res = new WordValue(String.valueOf(tc + 1));
        final IntermediateCode assign = new Move(true, res, new WordValue(Return.RET_SYM));
        head.link(assign);
        return Pair.of(st, new ICodeInfo(head, assign, res, tc + 1));
    }
}
