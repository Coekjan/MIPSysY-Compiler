package frontend;

import exceptions.SysYException;
import utils.Pair;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    @Override
    public SymbolTable check(SymbolTable symbolTable, boolean inLoop) throws SysYException {
        SymbolTable next = symbolTable;
        for (BlockItemNode item : items) {
            next = item.check(item instanceof BlockNode ? next.yield(Collections.emptyMap()) : next, inLoop);
        }
        return symbolTable;
    }

    @Override
    public Pair<SymbolTable, SyntaxNode> simplify(SymbolTable symbolTable) {
        SymbolTable next = symbolTable;
        final List<BlockItemNode> simBlkItem = new LinkedList<>();
        for (BlockItemNode item : items) {
            final Pair<SymbolTable, SyntaxNode> p =
                    item.simplify(item instanceof BlockNode ? next.yield(Collections.emptyMap()) : next);
            next = p.first;
            simBlkItem.add((BlockItemNode) p.second);
        }
        return Pair.of(symbolTable, new BlockNode(simBlkItem));
    }

    @Override
    public List<ReturnNode> findRet() {
        return items.stream().flatMap(i -> i.findRet().stream()).collect(Collectors.toList());
    }
}
