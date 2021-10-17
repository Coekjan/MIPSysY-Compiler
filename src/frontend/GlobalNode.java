package frontend;

import java.util.Collections;
import java.util.List;

public class GlobalNode {
    public final List<DeclNode> declNodes;
    public final List<FuncDefNode> funcDefNodes;
    public final FuncDefNode mainFuncDef;

    public GlobalNode(List<DeclNode> declNodes, List<FuncDefNode> funcDefNodes, FuncDefNode mainFuncDef) {
        this.declNodes = Collections.unmodifiableList(declNodes);
        this.funcDefNodes = Collections.unmodifiableList(funcDefNodes);
        this.mainFuncDef = mainFuncDef;
    }

    @Override
    public String toString() {
        return declNodes.stream().map(DeclNode::toString)
                .reduce((x, y) -> x + "\n" + y).orElse("") + "\n" +
                funcDefNodes.stream().map(FuncDefNode::toString)
                        .reduce((x, y) -> x + "\n" + y).orElse("") + "\n" + mainFuncDef.toString();
    }
}
