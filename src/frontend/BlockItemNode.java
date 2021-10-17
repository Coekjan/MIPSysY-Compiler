package frontend;

import java.util.Collections;
import java.util.List;

public interface BlockItemNode extends SyntaxNode {
    default List<ReturnNode> findRet() {
        return Collections.emptyList();
    }
}
