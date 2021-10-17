package frontend;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BlockNode implements StmtNode {
    public final List<BlockItemNode> items;

    public BlockNode(List<BlockItemNode> items) {
        this.items = Collections.unmodifiableList(items);
    }

    @Override
    public String toString() {
        return "{\n" + items.stream().map(Objects::toString)
                .reduce((x, y) -> x + "\n" + y).orElse("") + "\n}";
    }
}
