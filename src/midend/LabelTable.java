package midend;

import java.util.*;

public class LabelTable /* yielded by ast */ {
    private int labelCount = 0;
    private final Map<String, IntermediateCode> labelsMap = new HashMap<>();
    private final Map<IntermediateCode, List<String>> instMap = new HashMap<>();

    public IntermediateCode find(String label) {
        if (labelsMap.containsKey(label)) return labelsMap.get(label);
        throw new IllegalArgumentException();
    }

    public Optional<List<String>> find(IntermediateCode code) {
        if (instMap.containsKey(code)) return Optional.of(instMap.get(code));
        return Optional.empty();
    }

    public String createLabel() {
        return "@label_" + labelCount++;
    }

    public void assignLabelToCode(String label, IntermediateCode targetInstruction) {
        labelsMap.put(label, targetInstruction);
        if (!instMap.containsKey(targetInstruction)) {
            instMap.put(targetInstruction, new LinkedList<>());
        }
        instMap.get(targetInstruction).add(0, label);
    }
}
