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

    public void reassignCode(IntermediateCode bfr, IntermediateCode aft) {
        find(bfr).ifPresent(l -> {
            instMap.put(aft, new LinkedList<>(l));
            l.forEach(s -> labelsMap.put(s, aft));
        });
    }

    public void removeLabel(String label) {
        final IntermediateCode code = labelsMap.get(label);
        if (code != null) {
            labelsMap.remove(label);
            instMap.get(code).remove(label);
        }
    }

    public void minifyLabels(Collection<String> usedLabels) {
        final Set<String> unusedLabels = new HashSet<>();
        labelsMap.keySet().forEach(l -> {
            if (!usedLabels.contains(l) && l.startsWith("@")) {
                unusedLabels.add(l);
            }
        });
        unusedLabels.forEach(this::removeLabel);
    }
}
