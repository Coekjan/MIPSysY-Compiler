package midend;

import java.util.Collections;
import java.util.List;

public class Branch extends IntermediateCode implements Usage<Branch> {
    public final Value condition;
    public final String label;

    public Branch(Value condition, String label) {
        this.condition = condition;
        this.label = label;
    }

    @Override
    String display() {
        return "branch " + label + " if " + condition;
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        final int val = condition.get(machine);
        if (val != 0) {
            return labelTable.find(label);
        }
        return next;
    }

    @Override
    public List<Value> getUse() {
        return Collections.singletonList(condition);
    }

    @Override
    public Branch replaceUse(List<Value> uses) {
        return new Branch(uses.get(0), label);
    }
}
