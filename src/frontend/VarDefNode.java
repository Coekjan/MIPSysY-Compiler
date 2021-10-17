package frontend;

import utils.Pair;

import java.util.Map;

public interface VarDefNode extends SyntaxNode {
    Map<String, Pair<Boolean, Pair<ExprNode, ExprNode>>> getInfo();
}
