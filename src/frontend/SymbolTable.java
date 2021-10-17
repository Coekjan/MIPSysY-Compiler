package frontend;

import java.util.*;

public class SymbolTable {
    private final List<Map<String, VarDefNode>> variableMapList;
    public final Map<String, FuncDefNode> functionMap;

    public SymbolTable(Map<String, VarDefNode> variableMap, Map<String, FuncDefNode> functionMap) {
        this.variableMapList = Collections.singletonList(variableMap);
        this.functionMap = Collections.unmodifiableMap(functionMap);
    }

    public SymbolTable(SymbolTable symbolTable, Map<String, FuncDefNode> funcDefMap) {
        this.variableMapList = symbolTable.variableMapList;
        this.functionMap = Collections.unmodifiableMap(new HashMap<String, FuncDefNode>(funcDefMap) {{
            putAll(symbolTable.functionMap);
        }});
    }

    private SymbolTable(SymbolTable symbolTable, Map<String, VarDefNode> varDefNodeMap, boolean create) {
        this.variableMapList = create ? Collections.unmodifiableList(
                new ArrayList<Map<String, VarDefNode>>(symbolTable.variableMapList) {{
                    add(0, varDefNodeMap);
                }}) : Collections.unmodifiableList(
                        new ArrayList<Map<String, VarDefNode>>(symbolTable.variableMapList) {{
                            remove(0);
                            add(0, varDefNodeMap);
                        }});
        this.functionMap = symbolTable.functionMap;
    }

    public Map<String, VarDefNode> getHead() {
        return variableMapList.get(0);
    }

    public SymbolTable yield(Map<String, VarDefNode> varDefNodeMap) {
        return new SymbolTable(this, varDefNodeMap, true);
    }

    public SymbolTable update(Map<String, VarDefNode> varDefNodeMap) {
        return new SymbolTable(this, varDefNodeMap, false);
    }

    public Optional<VarDefNode> find(String name) {
        for (Map<String, VarDefNode> map : variableMapList) {
            if (map.containsKey(name)) {
                return Optional.of(map.get(name));
            }
        }
        return Optional.empty();
    }
}
