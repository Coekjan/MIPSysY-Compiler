package frontend;

import exceptions.SysYException;
import midend.LabelTable;
import utils.Pair;

import java.util.LinkedList;
import java.util.List;

public interface SyntaxNode {
    List<Pair<Integer, SysYException.Code>> errors = new LinkedList<>();

    SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException;

    // should be called after check
    Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable);

    Pair<SymbolTable, ICodeInfo> iCode(LabelTable lt, SymbolTable st, String lpBegin, String lpEnd, int tc);
}
