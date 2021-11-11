package frontend;

import exceptions.SysYException;
import midend.*;
import utils.Pair;

import static frontend.ConstNode.ZERO;

public class FuncParamNode implements SyntaxNode {
    public final String name;
    public final int line;
    public final Pair<ExprNode, ExprNode> dimensions;

    public FuncParamNode(Token identifier, Pair<ExprNode, ExprNode> dimensions) {
        this(identifier.content, identifier.line, dimensions);
    }

    public FuncParamNode(String name, int line, Pair<ExprNode, ExprNode> dimensions) {
        this.name = name;
        this.line = line;
        this.dimensions = dimensions;
    }

    @Override
    public String toString() {
        return name + dimensions.toString();
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        dimensions.first.check(symbolTable, inLoop);
        dimensions.second.check(symbolTable, inLoop);
        return symbolTable;
    }

    @Override
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        final ExprNode f = (ExprNode) dimensions.first.simplify(symbolTable).second;
        final ExprNode s = (ExprNode) dimensions.second.simplify(symbolTable).second;
        return Pair.of(symbolTable, new FuncParamNode(name, line, Pair.of(f, s)));
    }

    @Override
    public Pair<SymbolTable, ICodeInfo> iCode(LabelTable lt, SymbolTable st, String lpBegin, String lpEnd, int tc) {
        if (dimensions.first == ZERO && dimensions.second == ZERO) {
            final IntermediateCode word = new ParameterFetch(new WordValue(name + "%" + st.tellDepth()));
            return Pair.of(st, new ICodeInfo(word, word, null, tc));
        } else {
            final IntermediateCode addr = new ParameterFetch(new AddrValue(name + "%" + st.tellDepth()));
            return Pair.of(st, new ICodeInfo(addr, addr, null, tc));
        }
    }
}
