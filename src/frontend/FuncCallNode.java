package frontend;

import exceptions.SysYException;
import utils.Pair;

import java.util.*;

import static frontend.ConstNode.ZERO;

public class FuncCallNode implements ExprNode {
    public final String name;
    public final int line;
    public final List<ExprNode> arguments;

    public FuncCallNode(Token identifier, List<ExprNode> arguments) {
        this(identifier.content, identifier.line, arguments);
    }

    public FuncCallNode(String name, int line, List<ExprNode> arguments) {
        this.name = name;
        this.line = line;
        this.arguments = Collections.unmodifiableList(arguments);
    }

    @Override
    public String toString() {
        return name + "(" + arguments.stream()
                .map(Objects::toString).reduce((x, y) -> x + ", " + y).orElse("") + ")";
    }

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        final FuncDefNode funcDefNode = symbolTable.functionMap.get(name);
        final List<FuncParamNode> parameters = funcDefNode.parameters;
        if (arguments.size() != parameters.size()) {
            errors.add(Pair.of(line, name.equals("printf") ? SysYException.Code.l : SysYException.Code.d));
        }
        for (int i = 0, maxIndex = Math.min(arguments.size(), parameters.size()); i < maxIndex; ++i) {
            arguments.get(i).check(symbolTable, inLoop);
            if (parameters.get(i).dimensions.second != ZERO) {
                try {
                    if (arguments.get(i).getRetType(symbolTable) != ReturnType.DIM2) {
                        errors.add(Pair.of(line, SysYException.Code.e));
                    }
                } catch (SysYException e) {
                    errors.add(Pair.of(line, e.code));
                }
            } else if (parameters.get(i).dimensions.first != ZERO) {
                try {
                    if (arguments.get(i).getRetType(symbolTable) != ReturnType.DIM1) {
                        errors.add(Pair.of(line, SysYException.Code.e));
                    }
                } catch (SysYException e) {
                    errors.add(Pair.of(line, e.code));
                }
            } else {
                try {
                    if (arguments.get(i).getRetType(symbolTable) != ReturnType.INT) {
                        errors.add(Pair.of(line, SysYException.Code.e));
                    }
                } catch (SysYException e) {
                    errors.add(Pair.of(line, e.code));
                }
            }
        }
        return symbolTable;
    }

    @Override
    public ReturnType getRetType(SymbolTable symbolTable) throws SysYException {
        FuncDefNode defNode = symbolTable.functionMap.get(name);
        if (defNode != null) return defNode.returnInt ? ReturnType.INT : ReturnType.VOID;
        throw new SysYException(SysYException.Code.c);
    }
}
