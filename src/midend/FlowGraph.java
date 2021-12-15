package midend;

import java.util.*;

public class FlowGraph {
    private final Map<BasicBlock, List<BasicBlock>> blockNext = new HashMap<>();
    private final Map<BasicBlock, List<BasicBlock>> blockPrev = new HashMap<>();
    private final Map<String, BasicBlock> blockLabels = new HashMap<>();
    private final Map<IntermediateCode, BasicBlock> codeToBlock = new HashMap<>();

    public void link(BasicBlock from, BasicBlock to) {
        if (!blockNext.containsKey(from)) {
            blockNext.put(from, new ArrayList<>());
        }
        if (!blockPrev.containsKey(to)) {
            blockPrev.put(to, new ArrayList<>());
        }
        blockNext.get(from).add(to);
        blockPrev.get(to).add(from);
    }

    public void put(IntermediateCode code, BasicBlock block) {
        codeToBlock.put(code, block);
    }

    public BasicBlock getBlock(IntermediateCode code) {
        return codeToBlock.get(code);
    }

    public List<BasicBlock> nextOf(BasicBlock block) {
        return blockNext.getOrDefault(block, Collections.emptyList());
    }

    public List<BasicBlock> prevOf(BasicBlock block) {
        return blockPrev.getOrDefault(block, Collections.emptyList());
    }

    public void addBlockLabel(String label, BasicBlock block) {
        blockLabels.put(label, block);
    }

    public BasicBlock getBlockByLabel(String label) {
        return blockLabels.get(label);
    }
}
