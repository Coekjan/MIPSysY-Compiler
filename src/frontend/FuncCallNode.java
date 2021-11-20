package frontend;

import exceptions.SysYException;
import midend.*;
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
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        final List<ExprNode> simArgs = new LinkedList<>();
        for (ExprNode exprNode : arguments) {
            simArgs.add((ExprNode) exprNode.simplify(symbolTable).second);
        }
        return Pair.of(symbolTable, new FuncCallNode(name, line, simArgs));
    }

    @Override
    public Pair<SymbolTable, ICodeInfo> iCode(LabelTable lt, SymbolTable st, String lpBegin, String lpEnd, int tc) {
        int tempCount = tc;
        IntermediateCode last = new Nop();
        final IntermediateCode head = last;
        final List<Value> args = new LinkedList<>();
        for (int i = arguments.size() - 1; i >= 0; i--) {
            final ExprNode exprNode = arguments.get(i);
            final ICodeInfo code = exprNode.iCode(lt, st, lpBegin, lpEnd, tempCount).second;
            args.add(code.finalSym);
            tempCount = code.tempCount;
            last.link(code.first);
            last = code.second;
        }
        for (Value arg : args) {
            last = last.link(new PushArgument(arg));
        }
        final IntermediateCode call = new CallFunction(name);
        last.link(call);
        final WordValue ret = new WordValue(String.valueOf(tempCount + 1));
        final IntermediateCode getRet = new Move(true, ret, new WordValue(Return.RET_SYM));
        call.link(getRet);
        return Pair.of(st, new ICodeInfo(head, getRet, ret, tempCount + 1));
    }

    @Override
    public ReturnType getRetType(SymbolTable symbolTable) throws SysYException {
        FuncDefNode defNode = symbolTable.functionMap.get(name);
        if (defNode != null) return defNode.returnInt ? ReturnType.INT : ReturnType.VOID;
        throw new SysYException(SysYException.Code.c);
    }
}
