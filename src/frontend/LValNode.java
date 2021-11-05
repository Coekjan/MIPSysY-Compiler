package frontend;

import exceptions.SysYException;
import utils.Pair;

import java.util.Optional;
import java.util.stream.Collectors;

import static frontend.ConstNode.ZERO;

public class LValNode implements ExprNode {
    public final String name;
    public final int line;
    public final Pair<ExprNode, ExprNode> indexes;

    public LValNode(Token identifier, Pair<ExprNode, ExprNode> indexes) {
        this(identifier.content, identifier.line, indexes);
    }

    public LValNode(String name, int line, Pair<ExprNode, ExprNode> indexes) {
        this.name = name;
        this.line = line;
        this.indexes = indexes;
    }

    @Override
    public String toString() {
        return name + indexes.toString();
    }

    @Override
    public ReturnType getRetType(SymbolTable symbolTable) throws SysYException {
        final Optional<VarDefNode> defNode = symbolTable.find(name);
        if (defNode.isPresent()) {
            final Pair<ExprNode, ExprNode> defInfo = defNode.get().getInfo().get(name).second;
            final int dimDef = defInfo.first == ZERO ? 0 : (defInfo.second == ZERO ? 1 : 2);
            final int calDef = indexes.first == ZERO ? 0 : (indexes.second == ZERO ? 1 : 2);
            switch (dimDef - calDef) {
                case 0: return ReturnType.INT;
                case 1: return ReturnType.DIM1;
                case 2: return ReturnType.DIM2;
                default: throw new SysYException(SysYException.Code.e);
            }
        }
        throw new SysYException(SysYException.Code.c);
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        indexes.first.check(symbolTable, inLoop);
        indexes.second.check(symbolTable, inLoop);
        final Optional<VarDefNode> defNode = symbolTable.find(name);
        if (!defNode.isPresent()) errors.add(Pair.of(line, SysYException.Code.c));
        return symbolTable;
    }

    @Override
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        final Pair<ExprNode, ExprNode> p = Pair.of((ExprNode) indexes.first.simplify(symbolTable).second,
                (ExprNode) indexes.second.simplify(symbolTable).second);
        if (p.first instanceof ConstNode && p.second instanceof ConstNode) {
            final Optional<VarDefNode> defNode = symbolTable.find(name);
            assert defNode.isPresent();
            final VarDefNode varDefNode = defNode.get();
            if (!varDefNode.getInfo().get(name).first && varDefNode instanceof DeclNode) {
                final DeclNode declNode = (DeclNode) varDefNode;
                final DefNode initDef = declNode.defNodes.stream().filter(d -> d.name.equals(name))
                        .collect(Collectors.toList()).get(0);
                final int index = ((ConstNode) p.first).constant * ((ConstNode) initDef.dimensions.second).constant
                        + ((ConstNode) p.second).constant;
                return Pair.of(symbolTable, initDef.initValues.get(index));
            }
        }
        return Pair.of(symbolTable, new LValNode(name, line, p));
    }
}
